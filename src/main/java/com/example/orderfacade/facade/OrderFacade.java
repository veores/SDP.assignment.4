package com.example.orderfacade.facade;

import com.example.orderfacade.model.Cart;
import com.example.orderfacade.model.CartItem;
import com.example.orderfacade.model.Customer;
import com.example.orderfacade.model.Order;
import com.example.orderfacade.model.OrderStatus;
import com.example.orderfacade.subsystem.InventoryService;
import com.example.orderfacade.subsystem.NotificationException;
import com.example.orderfacade.subsystem.NotificationService;
import com.example.orderfacade.subsystem.OrderRepository;
import com.example.orderfacade.subsystem.OutOfStockException;
import com.example.orderfacade.subsystem.PaymentDeclinedException;
import com.example.orderfacade.subsystem.PaymentProcessor;
import com.example.orderfacade.subsystem.ShipmentAlreadyDispatchedException;
import com.example.orderfacade.subsystem.ShippingException;
import com.example.orderfacade.subsystem.ShippingService;
import com.example.orderfacade.subsystem.SubsystemException;
import com.example.orderfacade.subsystem.UnknownProductException;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class OrderFacade {

    private final InventoryService inventory;
    private final PaymentProcessor payments;
    private final ShippingService shipping;
    private final NotificationService notifications;
    private final OrderRepository orders;

    public OrderFacade(InventoryService inventory, PaymentProcessor payments,
                       ShippingService shipping, NotificationService notifications,
                       OrderRepository orders) {
        this.inventory = inventory;
        this.payments = payments;
        this.shipping = shipping;
        this.notifications = notifications;
        this.orders = orders;
    }

    public Order placeOrder(Cart cart, Customer customer) {
        requireValid(cart, customer);
        String orderId = orders.nextOrderId();
        List<CartItem> items = cart.items();
        Deque<Runnable> undoSteps = new ArrayDeque<>();

        try {
            BigDecimal total = inventory.calculateSubtotal(items)
                    .add(shipping.calculateFee(customer.address(), cart.totalUnits()));

            inventory.reserve(orderId, items);
            undoSteps.push(() -> inventory.release(orderId));

            String paymentId = payments.charge(customer.id(), total, orderId);
            undoSteps.push(() -> payments.refund(paymentId));

            String trackingNumber = shipping.schedule(orderId, customer.address(), items);

            Order order = new Order(orderId, customer, items, total, paymentId, trackingNumber);
            orders.save(order);
            notifyCustomer(order, "Order " + orderId + " confirmed",
                    "Thank you! Total charged: " + total + ". Tracking number: " + trackingNumber);
            return order;
        } catch (SubsystemException failure) {
            rollback(undoSteps);
            throw translate(failure);
        }
    }

    public Order cancelOrder(String orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderProcessingException(FailureReason.CANNOT_CANCEL,
                    "Order " + orderId + " is already cancelled");
        }

        try {
            shipping.cancel(order.getTrackingNumber());
        } catch (ShipmentAlreadyDispatchedException alreadyShipped) {
            throw new OrderProcessingException(FailureReason.CANNOT_CANCEL,
                    "Order " + orderId + " has already been shipped", alreadyShipped);
        }
        payments.refund(order.getPaymentId());
        inventory.release(orderId);

        order.markCancelled();
        orders.save(order);
        notifyCustomer(order, "Order " + orderId + " cancelled",
                "Your order was cancelled and " + order.getTotal() + " has been refunded.");
        return order;
    }

    public Order getOrder(String orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderProcessingException(
                FailureReason.ORDER_NOT_FOUND, "No such order: " + orderId));
    }

    public int getAvailableStock(String sku) {
        return inventory.availableStock(sku);
    }

    private void requireValid(Cart cart, Customer customer) {
        if (cart == null || cart.isEmpty()) {
            throw new OrderProcessingException(FailureReason.INVALID_REQUEST, "The cart is empty");
        }
        if (customer == null || customer.address() == null) {
            throw new OrderProcessingException(FailureReason.INVALID_REQUEST,
                    "A customer with an address is required");
        }
    }

    private void notifyCustomer(Order order, String subject, String body) {
        try {
            notifications.send(order.getCustomer().email(), subject, body);
        } catch (NotificationException failure) {
            order.addNote("Customer notification failed: " + failure.getMessage());
        }
    }

    private void rollback(Deque<Runnable> undoSteps) {
        while (!undoSteps.isEmpty()) {
            try {
                undoSteps.pop().run();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private OrderProcessingException translate(SubsystemException failure) {
        FailureReason reason;
        if (failure instanceof UnknownProductException) {
            reason = FailureReason.UNKNOWN_PRODUCT;
        } else if (failure instanceof OutOfStockException) {
            reason = FailureReason.OUT_OF_STOCK;
        } else if (failure instanceof PaymentDeclinedException) {
            reason = FailureReason.PAYMENT_DECLINED;
        } else if (failure instanceof ShippingException) {
            reason = FailureReason.SHIPPING_UNAVAILABLE;
        } else {
            reason = FailureReason.INVALID_REQUEST;
        }
        return new OrderProcessingException(reason, failure.getMessage(), failure);
    }
}

package com.example.orderfacade;

import com.example.orderfacade.facade.FailureReason;
import com.example.orderfacade.facade.OrderFacade;
import com.example.orderfacade.facade.OrderProcessingException;
import com.example.orderfacade.model.Address;
import com.example.orderfacade.model.Cart;
import com.example.orderfacade.model.Customer;
import com.example.orderfacade.model.Order;
import com.example.orderfacade.model.OrderStatus;
import com.example.orderfacade.subsystem.InventoryService;
import com.example.orderfacade.subsystem.NotificationService;
import com.example.orderfacade.subsystem.OrderRepository;
import com.example.orderfacade.subsystem.OutOfStockException;
import com.example.orderfacade.subsystem.PaymentProcessor;
import com.example.orderfacade.subsystem.ShippingService;

import java.math.BigDecimal;
import java.util.Set;

public class OrderFacadeTests {

    private static int passed = 0;
    private static int failed = 0;

    private InventoryService inventory;
    private PaymentProcessor payments;
    private ShippingService shipping;
    private NotificationService notifications;
    private OrderFacade facade;

    private final Customer berlinCustomer = new Customer("C-1", "Test Customer", "test@example.com",
            new Address("1 Main Street", "Berlin", "Germany"));

    private void setUp() {
        inventory = new InventoryService();
        inventory.registerProduct("A", new BigDecimal("100.00"), 5);
        inventory.registerProduct("B", new BigDecimal("20.00"), 1);
        payments = new PaymentProcessor();
        payments.openAccount("C-1", new BigDecimal("500.00"));
        shipping = new ShippingService("Germany", Set.of("Germany", "France"));
        notifications = new NotificationService();
        facade = new OrderFacade(inventory, payments, shipping, notifications, new OrderRepository());
    }

    void placeOrderCoordinatesAllSubsystems() {
        Order order = facade.placeOrder(new Cart().add("A", 2), berlinCustomer);

        check(new BigDecimal("207.00").equals(order.getTotal()), "total is items + shipping fee");
        check(inventory.availableStock("A") == 3, "stock was reserved");
        check(new BigDecimal("293.00").equals(payments.balanceOf("C-1")), "customer was charged");
        check(order.getTrackingNumber() != null, "shipment was scheduled");
        check(order.getStatus() == OrderStatus.PLACED, "order is PLACED");
        check(notifications.sentMessages().size() == 1, "confirmation e-mail was sent");
        check(facade.getOrder(order.getId()) == order, "order can be found again");
    }

    void outOfStockChangesNothing() {
        expectFailure(FailureReason.OUT_OF_STOCK, () -> facade.placeOrder(new Cart().add("B", 2), berlinCustomer));
        check(inventory.availableStock("B") == 1, "stock untouched");
        check(new BigDecimal("500.00").equals(payments.balanceOf("C-1")), "nobody was charged");
    }

    void partialReservationIsNeverLeftBehind() {
        expectFailure(FailureReason.OUT_OF_STOCK,
                () -> facade.placeOrder(new Cart().add("A", 1).add("B", 2), berlinCustomer));
        check(inventory.availableStock("A") == 5, "line A was not kept reserved");
    }

    void declinedPaymentReleasesStock() {
        payments.openAccount("C-1", new BigDecimal("10.00"));
        expectFailure(FailureReason.PAYMENT_DECLINED, () -> facade.placeOrder(new Cart().add("A", 1), berlinCustomer));
        check(inventory.availableStock("A") == 5, "reserved stock was released");
        check(new BigDecimal("10.00").equals(payments.balanceOf("C-1")), "balance unchanged");
        check(notifications.sentMessages().isEmpty(), "no e-mail for a failed order");
    }

    void shippingFailureRefundsPaymentAndReleasesStock() {
        Customer noStreet = new Customer("C-1", "Test Customer", "test@example.com",
                new Address(" ", "Berlin", "Germany"));
        expectFailure(FailureReason.SHIPPING_UNAVAILABLE, () -> facade.placeOrder(new Cart().add("A", 1), noStreet));
        check(inventory.availableStock("A") == 5, "stock released after shipping failure");
        check(new BigDecimal("500.00").equals(payments.balanceOf("C-1")), "payment refunded after shipping failure");
    }

    void unsupportedCountryIsRejectedBeforeAnythingHappens() {
        Customer abroad = new Customer("C-1", "Test Customer", "test@example.com",
                new Address("1 Beach Road", "Rio", "Brazil"));
        expectFailure(FailureReason.SHIPPING_UNAVAILABLE, () -> facade.placeOrder(new Cart().add("A", 1), abroad));
        check(inventory.availableStock("A") == 5, "stock untouched");
    }

    void unknownProductIsReported() {
        expectFailure(FailureReason.UNKNOWN_PRODUCT, () -> facade.placeOrder(new Cart().add("Z", 1), berlinCustomer));
    }

    void emptyCartIsInvalid() {
        expectFailure(FailureReason.INVALID_REQUEST, () -> facade.placeOrder(new Cart(), berlinCustomer));
    }

    void cancelOrderUndoesEverything() {
        Order order = facade.placeOrder(new Cart().add("A", 2), berlinCustomer);
        facade.cancelOrder(order.getId());

        check(order.getStatus() == OrderStatus.CANCELLED, "order is CANCELLED");
        check(inventory.availableStock("A") == 5, "stock returned");
        check(new BigDecimal("500.00").equals(payments.balanceOf("C-1")), "money refunded");
        check(shipping.isCancelled(order.getTrackingNumber()), "shipment cancelled");
        check(notifications.sentMessages().size() == 2, "cancellation e-mail sent");
    }

    void cancelTwiceIsRefused() {
        Order order = facade.placeOrder(new Cart().add("A", 1), berlinCustomer);
        facade.cancelOrder(order.getId());
        expectFailure(FailureReason.CANNOT_CANCEL, () -> facade.cancelOrder(order.getId()));
    }

    void dispatchedOrderCannotBeCancelled() {
        Order order = facade.placeOrder(new Cart().add("A", 1), berlinCustomer);
        shipping.dispatch(order.getTrackingNumber());

        expectFailure(FailureReason.CANNOT_CANCEL, () -> facade.cancelOrder(order.getId()));
        check(order.getStatus() == OrderStatus.PLACED, "order stays PLACED");
        check(inventory.availableStock("A") == 4, "stock stays reserved");
        check(new BigDecimal("394.00").equals(payments.balanceOf("C-1")), "no refund was made");
    }

    void unknownOrderCannotBeCancelled() {
        expectFailure(FailureReason.ORDER_NOT_FOUND, () -> facade.cancelOrder("ORD-9999"));
    }

    void failedNotificationDoesNotUndoTheOrder() {
        Customer badEmail = new Customer("C-1", "Test Customer", "not-an-email",
                new Address("1 Main Street", "Berlin", "Germany"));
        Order order = facade.placeOrder(new Cart().add("A", 1), badEmail);

        check(order.getStatus() == OrderStatus.PLACED, "order still placed");
        check(order.getNotes().size() == 1, "failure recorded as a note on the order");
        check(inventory.availableStock("A") == 4, "stock still reserved");
    }

    void subsystemClassesWorkWithoutTheFacade() {
        inventory.reserve("DIRECT-1", new Cart().add("A", 2).items());
        check(inventory.availableStock("A") == 3, "inventory works on its own");
        inventory.release("DIRECT-1");
        check(inventory.availableStock("A") == 5, "release works on its own");

        boolean thrown = false;
        try {
            inventory.reserve("DIRECT-2", new Cart().add("A", 99).items());
        } catch (OutOfStockException expected) {
            thrown = true;
        }
        check(thrown, "subsystem reports its own exception type when used directly");
    }

    void cartMergesSameSkuAndRejectsBadQuantity() {
        Cart cart = new Cart().add("A", 1).add("A", 2);
        check(cart.items().size() == 1 && cart.items().get(0).quantity() == 3, "same SKU is merged");

        boolean thrown = false;
        try {
            new Cart().add("A", 0);
        } catch (IllegalArgumentException expected) {
            thrown = true;
        }
        check(thrown, "zero quantity rejected");
    }

    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError(what);
        }
    }

    private static void expectFailure(FailureReason expected, Runnable action) {
        try {
            action.run();
        } catch (OrderProcessingException e) {
            check(e.getReason() == expected, "expected " + expected + " but got " + e.getReason());
            return;
        }
        throw new AssertionError("expected OrderProcessingException " + expected + " but nothing was thrown");
    }

    private void run(String name, Runnable test) {
        setUp();
        try {
            test.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable error) {
            failed++;
            System.out.println("FAIL  " + name + "  ->  " + error);
        }
    }

    public static void main(String[] args) {
        OrderFacadeTests t = new OrderFacadeTests();
        t.run("placeOrder coordinates all subsystems", t::placeOrderCoordinatesAllSubsystems);
        t.run("out of stock changes nothing", t::outOfStockChangesNothing);
        t.run("partial reservation is never left behind", t::partialReservationIsNeverLeftBehind);
        t.run("declined payment releases stock", t::declinedPaymentReleasesStock);
        t.run("shipping failure refunds payment and releases stock", t::shippingFailureRefundsPaymentAndReleasesStock);
        t.run("unsupported country rejected before anything happens", t::unsupportedCountryIsRejectedBeforeAnythingHappens);
        t.run("unknown product is reported", t::unknownProductIsReported);
        t.run("empty cart is invalid", t::emptyCartIsInvalid);
        t.run("cancelOrder undoes everything", t::cancelOrderUndoesEverything);
        t.run("cancelling twice is refused", t::cancelTwiceIsRefused);
        t.run("dispatched order cannot be cancelled", t::dispatchedOrderCannotBeCancelled);
        t.run("unknown order cannot be cancelled", t::unknownOrderCannotBeCancelled);
        t.run("failed notification does not undo the order", t::failedNotificationDoesNotUndoTheOrder);
        t.run("subsystem classes work without the facade", t::subsystemClassesWorkWithoutTheFacade);
        t.run("cart merges same SKU and rejects bad quantity", t::cartMergesSameSkuAndRejectsBadQuantity);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

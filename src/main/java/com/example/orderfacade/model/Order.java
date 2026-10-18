package com.example.orderfacade.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {

    private final String id;
    private final Customer customer;
    private final List<CartItem> items;
    private final BigDecimal total;
    private final String paymentId;
    private final String trackingNumber;
    private final List<String> notes = new ArrayList<>();
    private OrderStatus status = OrderStatus.PLACED;

    public Order(String id, Customer customer, List<CartItem> items, BigDecimal total,
                 String paymentId, String trackingNumber) {
        this.id = id;
        this.customer = customer;
        this.items = List.copyOf(items);
        this.total = total;
        this.paymentId = paymentId;
        this.trackingNumber = trackingNumber;
    }

    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public List<CartItem> getItems() { return items; }
    public BigDecimal getTotal() { return total; }
    public String getPaymentId() { return paymentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public OrderStatus getStatus() { return status; }
    public List<String> getNotes() { return Collections.unmodifiableList(notes); }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
    }

    public void addNote(String note) {
        notes.add(note);
    }

    @Override
    public String toString() {
        return "Order{" + id + ", " + status + ", total=" + total
                + ", tracking=" + trackingNumber + "}";
    }
}

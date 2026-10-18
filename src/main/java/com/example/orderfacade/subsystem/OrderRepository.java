package com.example.orderfacade.subsystem;

import com.example.orderfacade.model.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OrderRepository {

    private final Map<String, Order> orders = new HashMap<>();
    private int idCounter = 0;

    public String nextOrderId() {
        return String.format("ORD-%04d", ++idCounter);
    }

    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}

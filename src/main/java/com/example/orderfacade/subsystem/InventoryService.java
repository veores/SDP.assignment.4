package com.example.orderfacade.subsystem;

import com.example.orderfacade.model.CartItem;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryService {

    private final Map<String, Integer> stock = new HashMap<>();
    private final Map<String, BigDecimal> prices = new HashMap<>();
    private final Map<String, List<CartItem>> reservations = new HashMap<>();

    public void registerProduct(String sku, BigDecimal price, int initialStock) {
        prices.put(sku, price);
        stock.put(sku, initialStock);
    }

    public int availableStock(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    public BigDecimal priceOf(String sku) {
        BigDecimal price = prices.get(sku);
        if (price == null) {
            throw new UnknownProductException(sku);
        }
        return price;
    }

    public BigDecimal calculateSubtotal(List<CartItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            subtotal = subtotal.add(priceOf(item.sku()).multiply(BigDecimal.valueOf(item.quantity())));
        }
        return subtotal;
    }

    public void reserve(String orderId, List<CartItem> items) {
        for (CartItem item : items) {
            priceOf(item.sku());
            int available = availableStock(item.sku());
            if (available < item.quantity()) {
                throw new OutOfStockException(item.sku(), item.quantity(), available);
            }
        }
        for (CartItem item : items) {
            stock.merge(item.sku(), -item.quantity(), Integer::sum);
        }
        reservations.put(orderId, List.copyOf(items));
    }

    public void release(String orderId) {
        List<CartItem> reserved = reservations.remove(orderId);
        if (reserved == null) {
            return;
        }
        for (CartItem item : reserved) {
            stock.merge(item.sku(), item.quantity(), Integer::sum);
        }
    }

    public boolean hasReservation(String orderId) {
        return reservations.containsKey(orderId);
    }
}

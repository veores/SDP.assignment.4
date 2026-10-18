package com.example.orderfacade.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cart {

    private final List<CartItem> items = new ArrayList<>();

    public Cart add(String sku, int quantity) {
        CartItem newItem = new CartItem(sku, quantity);
        for (int i = 0; i < items.size(); i++) {
            CartItem existing = items.get(i);
            if (existing.sku().equals(sku)) {
                items.set(i, new CartItem(sku, existing.quantity() + quantity));
                return this;
            }
        }
        items.add(newItem);
        return this;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> items() {
        return Collections.unmodifiableList(items);
    }

    public int totalUnits() {
        return items.stream().mapToInt(CartItem::quantity).sum();
    }
}

package com.example.orderfacade.config;

import com.example.orderfacade.facade.OrderFacade;
import com.example.orderfacade.subsystem.InventoryService;
import com.example.orderfacade.subsystem.NotificationService;
import com.example.orderfacade.subsystem.OrderRepository;
import com.example.orderfacade.subsystem.PaymentProcessor;
import com.example.orderfacade.subsystem.ShippingService;

import java.math.BigDecimal;
import java.util.Set;

public final class ShopFactory {

    private ShopFactory() {
    }

    public static OrderFacade createDemoOrderFacade() {
        InventoryService inventory = new InventoryService();
        inventory.registerProduct("LAPTOP-15", new BigDecimal("899.99"), 5);
        inventory.registerProduct("PHONE-X", new BigDecimal("599.00"), 10);
        inventory.registerProduct("HEADPHONES", new BigDecimal("79.50"), 20);
        inventory.registerProduct("KEYBOARD", new BigDecimal("49.90"), 2);

        PaymentProcessor payments = new PaymentProcessor();
        payments.openAccount("C-100", new BigDecimal("2500.00"));
        payments.openAccount("C-200", new BigDecimal("50.00"));
        payments.openAccount("C-300", new BigDecimal("1000.00"));
        payments.openAccount("C-400", new BigDecimal("1000.00"));

        ShippingService shipping = new ShippingService("Germany", Set.of("Germany", "France", "Poland"));

        return new OrderFacade(inventory, payments, shipping, new NotificationService(), new OrderRepository());
    }
}

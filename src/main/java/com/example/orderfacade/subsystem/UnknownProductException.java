package com.example.orderfacade.subsystem;

public class UnknownProductException extends SubsystemException {

    public UnknownProductException(String sku) {
        super("Unknown product: " + sku);
    }
}

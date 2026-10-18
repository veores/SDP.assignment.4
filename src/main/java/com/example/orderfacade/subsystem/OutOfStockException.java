package com.example.orderfacade.subsystem;

public class OutOfStockException extends SubsystemException {

    public OutOfStockException(String sku, int requested, int available) {
        super("Not enough stock for " + sku + ": requested " + requested + ", available " + available);
    }
}

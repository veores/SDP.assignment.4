package com.example.orderfacade.subsystem;

public class ShipmentAlreadyDispatchedException extends ShippingException {

    public ShipmentAlreadyDispatchedException(String trackingNumber) {
        super("Shipment " + trackingNumber + " has already been dispatched and cannot be cancelled");
    }
}

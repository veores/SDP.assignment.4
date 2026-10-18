package com.example.orderfacade.subsystem;

import com.example.orderfacade.model.Address;
import com.example.orderfacade.model.CartItem;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShippingService {

    private static final BigDecimal DOMESTIC_BASE_FEE = new BigDecimal("5.00");
    private static final BigDecimal INTERNATIONAL_BASE_FEE = new BigDecimal("15.00");
    private static final BigDecimal FEE_PER_UNIT = new BigDecimal("1.00");

    private enum ShipmentStatus { SCHEDULED, DISPATCHED, CANCELLED }

    private final String homeCountry;
    private final Set<String> servedCountries;
    private final Map<String, ShipmentStatus> shipments = new HashMap<>();
    private int trackingCounter = 0;

    public ShippingService(String homeCountry, Set<String> servedCountries) {
        this.homeCountry = homeCountry;
        this.servedCountries = Set.copyOf(servedCountries);
    }

    public BigDecimal calculateFee(Address destination, int totalUnits) {
        if (!servedCountries.contains(destination.country())) {
            throw new ShippingException("We do not ship to " + destination.country());
        }
        BigDecimal baseFee = destination.country().equals(homeCountry)
                ? DOMESTIC_BASE_FEE : INTERNATIONAL_BASE_FEE;
        return baseFee.add(FEE_PER_UNIT.multiply(BigDecimal.valueOf(totalUnits)));
    }

    public String schedule(String orderId, Address destination, List<CartItem> items) {
        if (destination.street() == null || destination.street().isBlank()) {
            throw new ShippingException("Incomplete address for order " + orderId + ": street is missing");
        }
        String trackingNumber = String.format("TRK-%04d", ++trackingCounter);
        shipments.put(trackingNumber, ShipmentStatus.SCHEDULED);
        return trackingNumber;
    }

    public void dispatch(String trackingNumber) {
        requireKnown(trackingNumber);
        shipments.put(trackingNumber, ShipmentStatus.DISPATCHED);
    }

    public void cancel(String trackingNumber) {
        ShipmentStatus status = requireKnown(trackingNumber);
        if (status == ShipmentStatus.DISPATCHED) {
            throw new ShipmentAlreadyDispatchedException(trackingNumber);
        }
        shipments.put(trackingNumber, ShipmentStatus.CANCELLED);
    }

    public boolean isCancelled(String trackingNumber) {
        return shipments.get(trackingNumber) == ShipmentStatus.CANCELLED;
    }

    private ShipmentStatus requireKnown(String trackingNumber) {
        ShipmentStatus status = shipments.get(trackingNumber);
        if (status == null) {
            throw new ShippingException("Unknown shipment: " + trackingNumber);
        }
        return status;
    }
}

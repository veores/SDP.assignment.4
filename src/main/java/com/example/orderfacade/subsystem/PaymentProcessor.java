package com.example.orderfacade.subsystem;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PaymentProcessor {

    private static final class Payment {
        private final String customerId;
        private final BigDecimal amount;
        private boolean refunded;

        private Payment(String customerId, BigDecimal amount) {
            this.customerId = customerId;
            this.amount = amount;
        }
    }

    private final Map<String, BigDecimal> balances = new HashMap<>();
    private final Map<String, Payment> payments = new HashMap<>();
    private int paymentCounter = 0;

    public void openAccount(String customerId, BigDecimal initialBalance) {
        balances.put(customerId, initialBalance);
    }

    public BigDecimal balanceOf(String customerId) {
        return balances.getOrDefault(customerId, BigDecimal.ZERO);
    }

    public String charge(String customerId, BigDecimal amount, String orderId) {
        if (!balances.containsKey(customerId)) {
            throw new PaymentDeclinedException("No payment account for customer " + customerId);
        }
        if (amount.signum() <= 0) {
            throw new PaymentDeclinedException("Invalid amount for order " + orderId + ": " + amount);
        }
        BigDecimal balance = balances.get(customerId);
        if (balance.compareTo(amount) < 0) {
            throw new PaymentDeclinedException("Insufficient funds for order " + orderId
                    + ": need " + amount + ", have " + balance);
        }
        balances.put(customerId, balance.subtract(amount));
        String paymentId = String.format("PAY-%04d", ++paymentCounter);
        payments.put(paymentId, new Payment(customerId, amount));
        return paymentId;
    }

    public void refund(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Unknown payment: " + paymentId);
        }
        if (payment.refunded) {
            return;
        }
        balances.merge(payment.customerId, payment.amount, BigDecimal::add);
        payment.refunded = true;
    }
}

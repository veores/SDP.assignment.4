package com.example.orderfacade.facade;

public class OrderProcessingException extends RuntimeException {

    private final FailureReason reason;

    public OrderProcessingException(FailureReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public OrderProcessingException(FailureReason reason, String message) {
        this(reason, message, null);
    }

    public FailureReason getReason() {
        return reason;
    }
}

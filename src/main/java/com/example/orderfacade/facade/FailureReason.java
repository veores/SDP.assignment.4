package com.example.orderfacade.facade;

public enum FailureReason {
    INVALID_REQUEST,
    UNKNOWN_PRODUCT,
    OUT_OF_STOCK,
    PAYMENT_DECLINED,
    SHIPPING_UNAVAILABLE,
    ORDER_NOT_FOUND,
    CANNOT_CANCEL
}

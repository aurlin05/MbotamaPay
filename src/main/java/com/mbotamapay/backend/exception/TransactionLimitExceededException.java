package com.mbotamapay.backend.exception;

public class TransactionLimitExceededException extends BusinessException {
    public TransactionLimitExceededException(String message) {
        super(message);
    }
}

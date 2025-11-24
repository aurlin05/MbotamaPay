package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.payment.CinetPayCallback;
import com.mbotamapay.backend.dto.payment.PaymentInitRequest;
import com.mbotamapay.backend.dto.payment.PaymentInitResponse;
import com.mbotamapay.backend.entity.User;

public interface CinetPayService {
    PaymentInitResponse initializePayment(User user, PaymentInitRequest request);

    void handleCallback(CinetPayCallback callback);
}

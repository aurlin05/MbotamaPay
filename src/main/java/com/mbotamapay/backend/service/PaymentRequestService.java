package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.payment.PaymentRequestCreateRequest;
import com.mbotamapay.backend.dto.payment.PaymentRequestResponse;
import com.mbotamapay.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRequestService {
    PaymentRequestResponse createPaymentRequest(User requester, PaymentRequestCreateRequest request);

    PaymentRequestResponse acceptPaymentRequest(Long requestId, User payer);

    PaymentRequestResponse rejectPaymentRequest(Long requestId, User payer);

    Page<PaymentRequestResponse> getReceivedRequests(User payer, Pageable pageable);

    Page<PaymentRequestResponse> getSentRequests(User requester, Pageable pageable);
}

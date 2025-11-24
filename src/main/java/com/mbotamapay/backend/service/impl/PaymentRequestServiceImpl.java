package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.payment.PaymentRequestCreateRequest;
import com.mbotamapay.backend.dto.payment.PaymentRequestResponse;
import com.mbotamapay.backend.entity.PaymentRequest;
import com.mbotamapay.backend.entity.PaymentRequestStatus;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.exception.UserNotFoundException;
import com.mbotamapay.backend.repository.PaymentRequestRepository;
import com.mbotamapay.backend.repository.UserRepository;
import com.mbotamapay.backend.service.PaymentRequestService;
import com.mbotamapay.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestServiceImpl implements PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Override
    @Transactional
    public PaymentRequestResponse createPaymentRequest(User requester, PaymentRequestCreateRequest request) {
        User payer = userRepository.findByEmail(request.getPayerEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getPayerEmail()));

        if (requester.getId().equals(payer.getId())) {
            throw new BusinessException("Cannot request payment from yourself");
        }

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .requester(requester)
                .payer(payer)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(PaymentRequestStatus.PENDING)
                .build();

        PaymentRequest saved = paymentRequestRepository.save(paymentRequest);
        log.info("Payment request created: {} requests {} XAF from {}",
                requester.getEmail(), request.getAmount(), payer.getEmail());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentRequestResponse acceptPaymentRequest(Long requestId, User payer) {
        PaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Payment request not found"));

        if (!paymentRequest.getPayer().getId().equals(payer.getId())) {
            throw new BusinessException("You are not authorized to accept this request");
        }

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING) {
            throw new BusinessException("Payment request is not pending");
        }

        // Execute the transfer
        transactionService.sendMoneyByEmail(payer, paymentRequest.getRequester().getEmail(),
                paymentRequest.getAmount(), "Payment for request: " + paymentRequest.getDescription());

        paymentRequest.setStatus(PaymentRequestStatus.ACCEPTED);
        PaymentRequest updated = paymentRequestRepository.save(paymentRequest);

        log.info("Payment request accepted: {}", requestId);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public PaymentRequestResponse rejectPaymentRequest(Long requestId, User payer) {
        PaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Payment request not found"));

        if (!paymentRequest.getPayer().getId().equals(payer.getId())) {
            throw new BusinessException("You are not authorized to reject this request");
        }

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING) {
            throw new BusinessException("Payment request is not pending");
        }

        paymentRequest.setStatus(PaymentRequestStatus.REJECTED);
        PaymentRequest updated = paymentRequestRepository.save(paymentRequest);

        log.info("Payment request rejected: {}", requestId);
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentRequestResponse> getReceivedRequests(User payer, Pageable pageable) {
        return paymentRequestRepository.findByPayerAndStatusOrderByCreatedAtDesc(
                payer, PaymentRequestStatus.PENDING, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentRequestResponse> getSentRequests(User requester, Pageable pageable) {
        return paymentRequestRepository.findByRequesterOrderByCreatedAtDesc(requester, pageable)
                .map(this::toResponse);
    }

    private PaymentRequestResponse toResponse(PaymentRequest request) {
        return PaymentRequestResponse.builder()
                .id(request.getId())
                .requesterName(request.getRequester().getName())
                .requesterEmail(request.getRequester().getEmail())
                .payerName(request.getPayer().getName())
                .payerEmail(request.getPayer().getEmail())
                .amount(request.getAmount())
                .status(request.getStatus().name())
                .description(request.getDescription())
                .createdAt(request.getCreatedAt())
                .build();
    }
}

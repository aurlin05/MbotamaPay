package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.config.AppProperties;
import com.mbotamapay.backend.dto.payment.CinetPayCallback;
import com.mbotamapay.backend.dto.payment.PaymentInitRequest;
import com.mbotamapay.backend.dto.payment.PaymentInitResponse;
import com.mbotamapay.backend.entity.*;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.TransactionRepository;
import com.mbotamapay.backend.service.CinetPayService;
import com.mbotamapay.backend.service.WalletService;
import com.mbotamapay.backend.utils.CinetPaySignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinetPayServiceImpl implements CinetPayService {

    private final AppProperties appProperties;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final CinetPaySignatureValidator signatureValidator;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public PaymentInitResponse initializePayment(User user, PaymentInitRequest request) {
        log.info("Initializing CinetPay payment for user: {}", user.getEmail());

        String transactionId = "PAY-" + UUID.randomUUID().toString();
        String currency = request.getCurrency() != null ? request.getCurrency() : "XAF";

        // Create payment request to CinetPay
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("apikey", appProperties.getCinetpay().getApiKey());
        paymentData.put("site_id", appProperties.getCinetpay().getSiteId());
        paymentData.put("transaction_id", transactionId);
        paymentData.put("amount", request.getAmount());
        paymentData.put("currency", currency);
        paymentData.put("description", "Wallet Top-up - MbotamaPay");
        paymentData.put("customer_name", user.getName());
        paymentData.put("customer_email", user.getEmail());
        paymentData.put("customer_phone_number", user.getPhone());
        paymentData.put("notify_url", appProperties.getCinetpay().getNotifyUrl());
        paymentData.put("return_url", appProperties.getCinetpay().getReturnUrl());
        paymentData.put("channels", "ALL");
        paymentData.put("metadata", user.getId().toString());

        try {
            // Call CinetPay API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    appProperties.getCinetpay().getBaseUrl(),
                    paymentData,
                    Map.class);

            if (response != null && "ACCEPTED".equals(response.get("code"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");

                // Create pending transaction
                Wallet wallet = walletService.getWalletByUser(user);
                Transaction transaction = Transaction.builder()
                        .reference(transactionId)
                        .receiverWallet(wallet)
                        .amount(request.getAmount())
                        .type(TransactionType.TOP_UP)
                        .status(TransactionStatus.PENDING)
                        .description("CinetPay Top-up")
                        .build();
                transactionRepository.save(transaction);

                return PaymentInitResponse.builder()
                        .paymentUrl((String) data.get("payment_url"))
                        .transactionId(transactionId)
                        .paymentToken((String) data.get("payment_token"))
                        .build();
            } else {
                throw new BusinessException("Payment initialization failed");
            }
        } catch (Exception e) {
            log.error("Error initializing payment", e);
            throw new BusinessException("Failed to initialize payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleCallback(CinetPayCallback callback) {
        log.info("Processing CinetPay callback for transaction: {}", callback.getCpm_trans_id());

        // Validate signature
        String signatureData = callback.getCpm_trans_id() + callback.getCpm_amount() + callback.getCpm_currency();
        if (!signatureValidator.validateSignature(signatureData, callback.getSignature(),
                appProperties.getCinetpay().getApiKey())) {
            log.error("Invalid signature for CinetPay callback: {}", callback.getCpm_trans_id());
            throw new BusinessException("Invalid callback signature");
        }

        try {
            // Verify payment status with CinetPay
            Map<String, Object> verifyData = new HashMap<>();
            verifyData.put("apikey", appProperties.getCinetpay().getApiKey());
            verifyData.put("site_id", appProperties.getCinetpay().getSiteId());
            verifyData.put("transaction_id", callback.getCpm_trans_id());

            String verifyUrl = appProperties.getCinetpay().getBaseUrl().replace("/payment", "/check");
            @SuppressWarnings("unchecked")
            Map<String, Object> verifyResponse = restTemplate.postForObject(
                    verifyUrl,
                    verifyData,
                    Map.class);

            if (verifyResponse != null && "00".equals(verifyResponse.get("code"))) {
                Map<String, Object> data = (Map<String, Object>) verifyResponse.get("data");

                // Find transaction
                Transaction transaction = transactionRepository.findById(Long.parseLong(callback.getCpm_trans_id()))
                        .orElseThrow(() -> new BusinessException("Transaction not found"));

                if (transaction.getStatus() == TransactionStatus.PENDING) {
                    // Credit wallet
                    walletService.credit(transaction.getReceiverWallet().getId(), callback.getCpm_amount());

                    transaction.setStatus(TransactionStatus.SUCCESS);
                    transactionRepository.save(transaction);

                    log.info("Payment successful, wallet credited: {}", callback.getCpm_trans_id());
                }
            } else {
                log.error("Payment verification failed for: {}", callback.getCpm_trans_id());
            }
        } catch (Exception e) {
            log.error("Error processing callback", e);
        }
    }
}

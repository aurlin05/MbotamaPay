package com.mbotamapay.backend.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@Slf4j
public class CinetPaySignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public boolean validateSignature(String data, String receivedSignature, String apiKey) {
        try {
            String calculatedSignature = calculateSignature(data, apiKey);
            boolean isValid = calculatedSignature.equals(receivedSignature);

            if (!isValid) {
                log.warn("Invalid CinetPay signature. Expected: {}, Received: {}", calculatedSignature,
                        receivedSignature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating CinetPay signature", e);
            return false;
        }
    }

    public String calculateSignature(String data, String apiKey) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmac);
    }
}

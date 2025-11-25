package com.mbotamapay.backend.integrations.cinetpay;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for CinetPay Feign client
 */
public class CinetPayClientConfig {

    /**
     * Configure retry policy: 3 attempts with exponential backoff
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000, 3000, 3);
    }
}

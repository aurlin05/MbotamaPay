package com.mbotamapay.backend.integrations.cinetpay;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for CinetPay Feign client
 */
@Configuration
public class CinetPayClientConfig {

    /**
     * Configure retry policy: 3 attempts with exponential backoff
     */
    @Bean
    public Retryer cinetPayRetryer() {
        return new Retryer.Default(1000, 3000, 3);
    }
}

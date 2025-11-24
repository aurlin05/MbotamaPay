package com.mbotamapay.backend.integrations.feexpay;

import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for FeexPay Feign client
 */
@Configuration
public class FeexPayClientConfig {

    @Value("${feexpay.api-key}")
    private String apiKey;

    /**
     * Add Authorization header to all requests
     */
    @Bean
    public RequestInterceptor feexPayRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Authorization", "Bearer " + apiKey);
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
        };
    }

    /**
     * Configure retry policy: 3 attempts with exponential backoff
     */
    @Bean
    public Retryer feexPayRetryer() {
        return new Retryer.Default(1000, 3000, 3);
    }
}

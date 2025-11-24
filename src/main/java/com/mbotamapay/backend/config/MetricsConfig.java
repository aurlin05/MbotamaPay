package com.mbotamapay.backend.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter transactionCounter(MeterRegistry registry) {
        return Counter.builder("mbotamapay.transactions.total")
                .description("Total number of transactions")
                .tag("type", "all")
                .register(registry);
    }

    @Bean
    public Counter p2pCounter(MeterRegistry registry) {
        return Counter.builder("mbotamapay.transactions.p2p")
                .description("Total number of P2P transactions")
                .register(registry);
    }

    @Bean
    public Counter topupCounter(MeterRegistry registry) {
        return Counter.builder("mbotamapay.transactions.topup")
                .description("Total number of top-up transactions")
                .register(registry);
    }
}

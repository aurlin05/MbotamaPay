package com.mbotamapay.backend.config;

import com.mbotamapay.backend.routes.Routes;
import io.github.bucket4j.Bandwidth;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Getter
public class RateLimitConfig {

    private final Map<String, BandwidthConfig> rateLimitConfigs;

    public RateLimitConfig() {
        this.rateLimitConfigs = new HashMap<>();
        
        // Auth endpoints: 5 requests per minute per IP
        rateLimitConfigs.put(Routes.AUTH, new BandwidthConfig(5, Duration.ofMinutes(1), RateLimitType.IP));
        
        // Transaction endpoints: 10 requests per minute per user
        rateLimitConfigs.put(Routes.TRANSACTIONS, new BandwidthConfig(10, Duration.ofMinutes(1), RateLimitType.USER));
        rateLimitConfigs.put(Routes.WALLET, new BandwidthConfig(10, Duration.ofMinutes(1), RateLimitType.USER));
        rateLimitConfigs.put(Routes.PAYMENT, new BandwidthConfig(10, Duration.ofMinutes(1), RateLimitType.USER));
        rateLimitConfigs.put(Routes.PAYMENT_REQUESTS, new BandwidthConfig(10, Duration.ofMinutes(1), RateLimitType.USER));
        
        // Admin endpoints: 20 requests per minute per admin
        rateLimitConfigs.put(Routes.ADMIN, new BandwidthConfig(20, Duration.ofMinutes(1), RateLimitType.USER));
    }

    public BandwidthConfig getConfigForPath(String path) {
        return rateLimitConfigs.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    @Getter
    public static class BandwidthConfig {
        private final long capacity;
        private final Duration refillDuration;
        private final RateLimitType type;

        public BandwidthConfig(long capacity, Duration refillDuration, RateLimitType type) {
            this.capacity = capacity;
            this.refillDuration = refillDuration;
            this.type = type;
        }

        public Bandwidth toBandwidth() {
            return Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(capacity, refillDuration)
                    .build();
        }
    }

    public enum RateLimitType {
        IP,    // Rate limit by IP address
        USER   // Rate limit by authenticated user
    }
}

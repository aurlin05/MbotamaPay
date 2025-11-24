package com.mbotamapay.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbotamapay.backend.config.RateLimitConfig;
import com.mbotamapay.backend.exception.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;
    
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        RateLimitConfig.BandwidthConfig config = rateLimitConfig.getConfigForPath(path);

        // Apply rate limiting if configuration exists for this path
        if (config != null) {
            String key = getRateLimitKey(request, config);
            Bucket bucket = resolveBucket(key, config);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            
            if (probe.isConsumed()) {
                addRateLimitHeaders(response, probe);
                filterChain.doFilter(request, response);
            } else {
                log.warn("Rate limit exceeded for key: {}", key);
                handleRateLimitExceeded(request, response, probe);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String getRateLimitKey(HttpServletRequest request, RateLimitConfig.BandwidthConfig config) {
        if (config.getType() == RateLimitConfig.RateLimitType.USER) {
            // For user-based rate limiting, use authenticated user ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                return "user:" + authentication.getName();
            }
            // Fall back to IP if user is not authenticated
            return "ip:" + getClientIP(request);
        } else {
            // For IP-based rate limiting
            return "ip:" + getClientIP(request);
        }
    }

    private Bucket resolveBucket(String key, RateLimitConfig.BandwidthConfig config) {
        return cache.computeIfAbsent(key, k -> createNewBucket(config));
    }

    private Bucket createNewBucket(RateLimitConfig.BandwidthConfig config) {
        return Bucket.builder()
                .addLimit(config.toBandwidth())
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private void addRateLimitHeaders(HttpServletResponse response, ConsumptionProbe probe) {
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        // Calculate reset time based on nanoseconds until refill
        long resetSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        response.setHeader("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + resetSeconds));
    }

    private long getRetryAfterSeconds(ConsumptionProbe probe) {
        return (probe.getNanosToWaitForRefill() / 1_000_000_000) + 1;
    }
    
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                         ConsumptionProbe probe) throws IOException {
        long retryAfterSeconds = getRetryAfterSeconds(probe);
        
        // Set HTTP status and headers
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfterSeconds));
        
        // Get correlation ID from MDC
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId == null) {
            correlationId = "N/A";
        }
        
        // Build structured error response
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .code("RATE_LIMIT_EXCEEDED")
                .message("Rate limit exceeded. Please retry after " + retryAfterSeconds + " seconds.")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        // Write JSON response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

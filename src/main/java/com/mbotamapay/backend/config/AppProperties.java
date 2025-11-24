package com.mbotamapay.backend.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    @NotNull(message = "JWT configuration is required")
    private JwtProperties jwt;

    @NotNull(message = "CinetPay configuration is required")
    private CinetPayPropertiesNested cinetpay;

    @NotNull(message = "Mail configuration is required")
    private MailProperties mail;

    @PostConstruct
    public void validate() {
        validateJwtProperties();
        validateCinetPayProperties();
        validateMailProperties();
    }

    private void validateJwtProperties() {
        if (jwt == null) {
            throw new IllegalStateException("JWT configuration is missing. Please set app.jwt.* properties.");
        }
        if (!StringUtils.hasText(jwt.getSecret())) {
            throw new IllegalStateException("JWT secret is required. Please set JWT_SECRET environment variable.");
        }
        if (jwt.getSecret().length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long for security.");
        }
        if (jwt.getExpirationMs() == null || jwt.getExpirationMs() <= 0) {
            throw new IllegalStateException("JWT expiration time must be positive.");
        }
    }

    private void validateCinetPayProperties() {
        if (cinetpay == null) {
            throw new IllegalStateException("CinetPay configuration is missing. Please set app.cinetpay.* properties.");
        }
        if (!StringUtils.hasText(cinetpay.getApiKey())) {
            throw new IllegalStateException("CinetPay API key is required. Please set CINETPAY_API_KEY environment variable.");
        }
        if (!StringUtils.hasText(cinetpay.getSiteId())) {
            throw new IllegalStateException("CinetPay site ID is required. Please set CINETPAY_SITE_ID environment variable.");
        }
        if (!StringUtils.hasText(cinetpay.getNotifyUrl())) {
            throw new IllegalStateException("CinetPay notify URL is required. Please set CINETPAY_NOTIFY_URL environment variable.");
        }
        if (!StringUtils.hasText(cinetpay.getReturnUrl())) {
            throw new IllegalStateException("CinetPay return URL is required. Please set CINETPAY_RETURN_URL environment variable.");
        }
    }

    private void validateMailProperties() {
        if (mail == null) {
            throw new IllegalStateException("Mail configuration is missing. Please set app.mail.* properties.");
        }
        if (!StringUtils.hasText(mail.getFrom())) {
            throw new IllegalStateException("Mail from address is required. Please set MAIL_FROM environment variable.");
        }
    }

    @Data
    public static class JwtProperties {
        @NotBlank(message = "JWT secret is required")
        private String secret;

        @NotNull(message = "JWT expiration time is required")
        private Long expirationMs;
    }

    @Data
    public static class CinetPayPropertiesNested {
        @NotBlank(message = "CinetPay API key is required")
        private String apiKey;

        @NotBlank(message = "CinetPay site ID is required")
        private String siteId;

        @NotBlank(message = "CinetPay base URL is required")
        private String baseUrl;

        @NotBlank(message = "CinetPay notify URL is required")
        private String notifyUrl;

        @NotBlank(message = "CinetPay return URL is required")
        private String returnUrl;
    }

    @Data
    public static class MailProperties {
        @NotBlank(message = "Mail from address is required")
        private String from;
    }
}

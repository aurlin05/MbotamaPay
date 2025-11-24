package com.mbotamapay.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // HSTS - Force HTTPS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // XSS Protection
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Content Security Policy
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https:; " +
                        "font-src 'self' data:;");

        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=()");

        filterChain.doFilter(request, response);
    }
}

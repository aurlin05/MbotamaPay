package com.mbotamapay.backend.integrations;

import com.mbotamapay.backend.integrations.cinetpay.CinetPayProvider;
import com.mbotamapay.backend.integrations.feexpay.FeexPayProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProviderFactory {

    private final FeexPayProvider feexPayProvider;
    private final CinetPayProvider cinetPayProvider;

    /**
     * Get provider by name
     */
    public PaymentProvider getProvider(String providerName) {
        if ("FEEXPAY".equalsIgnoreCase(providerName)) {
            return feexPayProvider;
        } else if ("CINETPAY".equalsIgnoreCase(providerName)) {
            return cinetPayProvider;
        }
        throw new IllegalArgumentException("Unknown provider: " + providerName);
    }

    /**
     * Get best provider for a country
     * Logic can be enhanced to support failover or load balancing
     */
    public PaymentProvider getProviderForCountry(String countryCode) {
        // Preference logic
        if ("BJ".equalsIgnoreCase(countryCode) || "TG".equalsIgnoreCase(countryCode)) {
            return feexPayProvider; // FeexPay preferred for Benin/Togo
        } else if ("CI".equalsIgnoreCase(countryCode) || "SN".equalsIgnoreCase(countryCode)) {
            return cinetPayProvider; // CinetPay preferred for Ivory Coast/Senegal
        }

        // Fallback checks
        if (feexPayProvider.supportsCountry(countryCode)) {
            return feexPayProvider;
        }
        if (cinetPayProvider.supportsCountry(countryCode)) {
            return cinetPayProvider;
        }

        // Default fallback
        return feexPayProvider;
    }

    /**
     * Get all available providers
     */
    public List<PaymentProvider> getAllProviders() {
        return List.of(feexPayProvider, cinetPayProvider);
    }
}

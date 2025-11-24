package com.mbotamapay.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // Support for multiple regional formats
    // Cameroon: +237XXXXXXXXX or 237XXXXXXXXX or XXXXXXXXX (9 digits)
    // International: +[country code][number] (E.164 format)
    private static final String PHONE_PATTERN = 
        "^(\\+?237)?[6-9]\\d{8}$|^\\+\\d{1,3}\\d{4,14}$";
    
    private static final Pattern pattern = Pattern.compile(PHONE_PATTERN);

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Remove spaces and dashes for validation
        String cleanedPhone = phoneNumber.replaceAll("[\\s-]", "");

        // Check length constraints (E.164 format: max 15 digits including country code)
        if (cleanedPhone.length() > 16) { // +15 digits
            return false;
        }

        // Check format using regex
        return pattern.matcher(cleanedPhone).matches();
    }
}

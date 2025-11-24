package com.mbotamapay.backend.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberValidatorTest {

    private PhoneNumberValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new PhoneNumberValidator();
    }

    @Test
    void testValidCameroonPhoneNumbers() {
        assertTrue(validator.isValid("677123456", context));
        assertTrue(validator.isValid("237677123456", context));
        assertTrue(validator.isValid("+237677123456", context));
        assertTrue(validator.isValid("6 77 12 34 56", context)); // with spaces
    }

    @Test
    void testValidInternationalPhoneNumbers() {
        assertTrue(validator.isValid("+33612345678", context));
        assertTrue(validator.isValid("+1234567890", context));
    }

    @Test
    void testInvalidPhoneNumbers() {
        assertFalse(validator.isValid("", context));
        assertFalse(validator.isValid("123", context));
        assertFalse(validator.isValid("invalid", context));
        assertFalse(validator.isValid(null, context));
    }
}

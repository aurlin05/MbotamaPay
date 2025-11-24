package com.mbotamapay.backend.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {

    private EmailValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new EmailValidator();
    }

    @Test
    void testValidEmail() {
        assertTrue(validator.isValid("test@example.com", context));
        assertTrue(validator.isValid("user.name@domain.co", context));
        assertTrue(validator.isValid("user+tag@example.com", context));
    }

    @Test
    void testInvalidEmail() {
        assertFalse(validator.isValid("", context));
        assertFalse(validator.isValid("invalid", context));
        assertFalse(validator.isValid("@example.com", context));
        assertFalse(validator.isValid("user@", context));
        assertFalse(validator.isValid(null, context));
    }
}

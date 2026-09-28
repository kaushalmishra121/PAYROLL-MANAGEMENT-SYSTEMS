package com.payroll.service;

import com.payroll.exception.ValidationException;
import com.payroll.util.ValidationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeValidationTest {

    @Test
    @DisplayName("Valid emails should pass without exception")
    void testValidEmails() {
        assertDoesNotThrow(() -> ValidationUtils.validateEmail("john.doe@enterprise.com"));
        assertDoesNotThrow(() -> ValidationUtils.validateEmail("alex_wright123@global.org"));
    }

    @Test
    @DisplayName("Invalid emails should throw ValidationException")
    void testInvalidEmails() {
        assertThrows(ValidationException.class, () -> ValidationUtils.validateEmail("invalid-email"));
        assertThrows(ValidationException.class, () -> ValidationUtils.validateEmail("test@"));
        assertThrows(ValidationException.class, () -> ValidationUtils.validateEmail("@example.com"));
        assertThrows(ValidationException.class, () -> ValidationUtils.validateEmail(""));
    }

    @Test
    @DisplayName("Blank required strings should throw ValidationException")
    void testRequireNonBlank() {
        assertThrows(ValidationException.class, () -> ValidationUtils.requireNonBlank(null, "Name"));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireNonBlank("   ", "Name"));
        assertDoesNotThrow(() -> ValidationUtils.requireNonBlank("Alexander", "Name"));
    }

    @Test
    @DisplayName("Monetary validation: negative values must throw ValidationException")
    void testMonetaryValidation() {
        assertThrows(ValidationException.class, () -> ValidationUtils.validatePositiveOrZero(new BigDecimal("-10.00"), "Basic Salary"));
        assertThrows(ValidationException.class, () -> ValidationUtils.validatePositive(BigDecimal.ZERO, "Basic Salary"));
        assertDoesNotThrow(() -> ValidationUtils.validatePositive(new BigDecimal("100.00"), "Basic Salary"));
    }
}

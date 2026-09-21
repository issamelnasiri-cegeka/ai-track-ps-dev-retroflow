package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.exception.InvalidInputException;

final class ServiceValidation {

    private ServiceValidation() {
    }

    static void requireId(Long id, String field) {
        if (id == null || id <= 0) {
            throw new InvalidInputException(field + " must be positive");
        }
    }

    static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(field + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new InvalidInputException(field + " must be at most " + maxLength + " characters");
        }
    }
}

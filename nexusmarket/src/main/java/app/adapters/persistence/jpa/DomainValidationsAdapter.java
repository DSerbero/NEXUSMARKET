package app.adapters.persistence.jpa;

import app.domain.exceptions.DomainValidationException;

/**
 * Small validation helpers shared by JPA adapters. These guard adapter-level
 * invariants (presence of required fields); business rules stay in the domain
 * services.
 */
final class DomainValidationsAdapter {

    private DomainValidationsAdapter() {
    }

    static <T> T require(T value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }
}

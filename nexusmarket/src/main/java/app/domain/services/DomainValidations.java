package app.domain.services;

import app.domain.models.User;
import app.domain.services.exceptions.DomainValidationException;
import app.domain.valueObjects.UserStatus;

final class DomainValidations {

    private DomainValidations() {
    }

    static <T> T required(T value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    static void active(User user) {
        required(user, "user");
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new DomainValidationException("User must be active");
        }
    }
}

package application.domain.services;

import application.domain.models.User;
import application.domain.exceptions.DomainValidationException;
import application.domain.valueobjects.UserStatus;

public final class DomainValidations {

    private DomainValidations() {
    }

    public static <T> T required(T value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    public static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    public static void active(User user) {
        required(user, "user");
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new DomainValidationException("User must be active");
        }
    }

    public static boolean sameIdentity(User first, User second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.equals(second)) {
            return true;
        }
        String firstIdentifier = first.getIdentifier();
        return firstIdentifier != null && !firstIdentifier.isBlank()
                && firstIdentifier.equals(second.getIdentifier());
    }
}

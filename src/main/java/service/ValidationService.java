package service;

import model.Customer;
import model.Restaurant;

import java.time.LocalDateTime;

/**
 * Shared validation rules for service methods and UI-facing workflows.
 */
public class ValidationService {
    public boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public boolean isValidEmail(String email) {
        return isNonBlank(email) && email.contains("@") && email.contains(".");
    }

    public boolean isValidPartySize(int partySize) {
        return partySize > 0;
    }

    public boolean isFutureDateTime(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(LocalDateTime.now());
    }

    public boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    public void validateReview(Customer customer, Restaurant restaurant, int rating, String comment) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant is required.");
        }
        if (!isValidRating(rating)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (!isNonBlank(comment)) {
            throw new IllegalArgumentException("Review comment is required.");
        }
    }

    public void validateAvailabilitySearch(
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant is required.");
        }
        if (!isFutureDateTime(dateTime)) {
            throw new IllegalArgumentException("Reservation date and time must be in the future.");
        }
        if (!isValidPartySize(partySize)) {
            throw new IllegalArgumentException("Party size must be positive.");
        }
        if (partySize > restaurant.getMaxCapacity()) {
            throw new IllegalArgumentException("Party size cannot exceed restaurant maximum capacity.");
        }
    }

    public void validateReservationRequest(
            Customer customer,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        validateAvailabilitySearch(restaurant, dateTime, partySize);
    }
}

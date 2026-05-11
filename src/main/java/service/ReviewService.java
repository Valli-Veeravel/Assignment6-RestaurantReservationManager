package service;

import model.Customer;
import model.Restaurant;
import model.Review;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ReviewService {
    private final DataStore dataStore;
    private final ValidationService validationService;

    public ReviewService(DataStore dataStore, ValidationService validationService) {
        this.dataStore = dataStore;
        this.validationService = validationService;
    }

    public List<Review> getReviews() {
        return dataStore.getAllReviews().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .toList();
    }

    public List<Review> getReviewsForRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant is required.");
        }
        return dataStore.getAllReviews().stream()
                .filter(review -> review.getRestaurant() != null)
                .filter(review -> restaurant.getRestaurantId().equals(review.getRestaurant().getRestaurantId()))
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .toList();
    }

    public Review submitReview(Customer customer, Restaurant restaurant, int rating, String comment) {
        validationService.validateReview(customer, restaurant, rating, comment);
        Review review = new Review(UUID.randomUUID().toString(), customer, restaurant, rating, comment.trim());
        restaurant.addReview(review);
        dataStore.addReview(review);
        return review;
    }
}

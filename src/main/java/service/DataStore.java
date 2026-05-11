package service;

import model.Customer;
import model.Reservation;
import model.Restaurant;
import model.Review;
import model.Staff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DataStore {
    private final Map<String, Customer> customers;
    private final Map<String, Staff> staffMembers;
    private final Map<String, Restaurant> restaurants;
    private final Map<String, Reservation> reservations;
    private final Map<String, Review> reviews;

    public DataStore() {
        this.customers = new LinkedHashMap<>();
        this.staffMembers = new LinkedHashMap<>();
        this.restaurants = new LinkedHashMap<>();
        this.reservations = new LinkedHashMap<>();
        this.reviews = new LinkedHashMap<>();
    }

    public void addCustomer(Customer customer) {
        customers.put(customer.getCustomerId(), customer);
    }

    public Optional<Customer> findCustomerById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    public Collection<Customer> getAllCustomers() {
        return new ArrayList<>(customers.values());
    }

    public void addStaff(Staff staff) {
        staffMembers.put(staff.getStaffId(), staff);
    }

    public Optional<Staff> findStaffById(String staffId) {
        return Optional.ofNullable(staffMembers.get(staffId));
    }

    public Collection<Staff> getAllStaffMembers() {
        return new ArrayList<>(staffMembers.values());
    }

    public void addRestaurant(Restaurant restaurant) {
        restaurants.put(restaurant.getRestaurantId(), restaurant);
    }

    public Optional<Restaurant> findRestaurantById(String restaurantId) {
        return Optional.ofNullable(restaurants.get(restaurantId));
    }

    public Collection<Restaurant> getAllRestaurants() {
        return new ArrayList<>(restaurants.values());
    }

    public void addReservation(Reservation reservation) {
        reservations.put(reservation.getReservationId(), reservation);
    }

    public Optional<Reservation> findReservationById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    public Collection<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    public void addReview(Review review) {
        reviews.put(review.getReviewId(), review);
    }

    public Optional<Review> findReviewById(String reviewId) {
        return Optional.ofNullable(reviews.get(reviewId));
    }

    public Collection<Review> getAllReviews() {
        return new ArrayList<>(reviews.values());
    }
}

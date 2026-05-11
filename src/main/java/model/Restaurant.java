package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Restaurant {
    private String restaurantId;
    private String name;
    private String address;
    private int maxCapacity;
    private AvailabilitySchedule availabilitySchedule;
    private Waitlist waitlist;
    private List<Reservation> reservations;
    private List<Review> reviews;

    public Restaurant(String restaurantId, String name, String address, int maxCapacity) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.address = address;
        this.maxCapacity = maxCapacity;
        this.availabilitySchedule = new AvailabilitySchedule(restaurantId + "-schedule");
        this.waitlist = new Waitlist(restaurantId + "-waitlist");
        this.reservations = new ArrayList<>();
        this.reviews = new ArrayList<>();
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public AvailabilitySchedule getAvailabilitySchedule() {
        return availabilitySchedule;
    }

    public void setAvailabilitySchedule(AvailabilitySchedule availabilitySchedule) {
        this.availabilitySchedule = Objects.requireNonNull(availabilitySchedule);
    }

    public Waitlist getWaitlist() {
        return waitlist;
    }

    public void setWaitlist(Waitlist waitlist) {
        this.waitlist = Objects.requireNonNull(waitlist);
    }

    public List<Reservation> getReservations() {
        return Collections.unmodifiableList(reservations);
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = new ArrayList<>(reservations);
    }

    public List<Review> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = new ArrayList<>(reviews);
    }

    public boolean getAvailability(LocalDateTime dateTime, int partySize) {
        return availabilitySchedule.hasCapacity(dateTime, partySize);
    }

    public List<Reservation> getReservations(LocalDateTime dateTime) {
        return reservations.stream()
                .filter(reservation -> reservation.getDateTime().equals(dateTime))
                .collect(Collectors.toList());
    }

    public void addReservation(Reservation reservation) {
        if (reservation != null && !reservations.contains(reservation)) {
            reservations.add(reservation);
        }
    }

    public boolean removeReservation(Reservation reservation) {
        return reservations.remove(reservation);
    }

    public void addReview(Review review) {
        if (review != null && !reviews.contains(review)) {
            reviews.add(review);
        }
    }
}

package model;

import service.ReservationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Customer extends User {
    private String customerId;
    private List<String> reservationIds;

    public Customer(String userId, String customerId, String name, String email, String phoneNumber) {
        this(userId, customerId, name, email, phoneNumber, "password");
    }

    public Customer(String userId, String customerId, String name, String email, String phoneNumber, String password) {
        super(userId, name, email, phoneNumber, password);
        this.customerId = customerId;
        this.reservationIds = new ArrayList<>();
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<String> getReservationIds() {
        return Collections.unmodifiableList(reservationIds);
    }

    public void setReservationIds(List<String> reservationIds) {
        this.reservationIds = new ArrayList<>(reservationIds);
    }

    public void addReservationId(String reservationId) {
        if (!reservationIds.contains(reservationId)) {
            reservationIds.add(reservationId);
        }
    }

    public void removeReservationId(String reservationId) {
        reservationIds.remove(reservationId);
    }

    public Optional<Reservation> requestReservation(
            ReservationManager manager,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        return manager.createRequest(this, restaurant, dateTime, partySize);
    }

    public boolean cancelReservation(ReservationManager manager, String reservationId) {
        return manager.cancelReservation(reservationId);
    }

    public boolean searchAvailability(Restaurant restaurant, LocalDateTime dateTime, int partySize) {
        return restaurant.getAvailability(dateTime, partySize);
    }

    public boolean searchAvailability(
            ReservationManager manager,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        return manager.searchAvailability(restaurant, dateTime, partySize);
    }
}

package model;

import java.time.LocalDateTime;

public class Reservation {
    private String reservationId;
    private Customer customer;
    private Restaurant restaurant;
    private LocalDateTime dateTime;
    private int partySize;
    private ReservationStatus status;

    public Reservation(
            String reservationId,
            Customer customer,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        this(reservationId, customer, restaurant, dateTime, partySize, ReservationStatus.PENDING);
    }

    public Reservation(
            String reservationId,
            Customer customer,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize,
            ReservationStatus status
    ) {
        this.reservationId = reservationId;
        this.customer = customer;
        this.restaurant = restaurant;
        this.dateTime = dateTime;
        this.partySize = partySize;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public void updateStatus(ReservationStatus newStatus) {
        this.status = newStatus;
    }

    public boolean confirmDetails() {
        return reservationId != null
                && customer != null
                && restaurant != null
                && dateTime != null
                && partySize > 0
                && status != null;
    }
}

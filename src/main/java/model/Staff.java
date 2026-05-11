package model;

import service.ReservationManager;

import java.util.Optional;

public class Staff extends User {
    private String staffId;
    private String role;
    private Restaurant restaurant;

    public Staff(String userId, String staffId, String name, String email, String phoneNumber, String role) {
        this(userId, staffId, name, email, phoneNumber, role, null, "password");
    }

    public Staff(
            String userId,
            String staffId,
            String name,
            String email,
            String phoneNumber,
            String role,
            Restaurant restaurant
    ) {
        this(userId, staffId, name, email, phoneNumber, role, restaurant, "password");
    }

    public Staff(
            String userId,
            String staffId,
            String name,
            String email,
            String phoneNumber,
            String role,
            Restaurant restaurant,
            String password
    ) {
        super(userId, name, email, phoneNumber, password);
        this.staffId = staffId;
        this.role = role;
        this.restaurant = restaurant;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Optional<Reservation> reviewReservation(ReservationManager manager, String reservationId) {
        return manager.findReservation(reservationId);
    }

    public boolean approveReservation(ReservationManager manager, String reservationId) {
        return manager.acceptReservation(this, reservationId);
    }

    public boolean denyReservation(ReservationManager manager, String reservationId) {
        return manager.denyReservation(this, reservationId);
    }
}

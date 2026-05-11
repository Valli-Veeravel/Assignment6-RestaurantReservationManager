package model;

import service.ReservationManager;

import java.util.Optional;

public class Staff extends User {
    private String staffId;
    private String role;

    public Staff(String userId, String staffId, String name, String email, String phoneNumber, String role) {
        super(userId, name, email, phoneNumber);
        this.staffId = staffId;
        this.role = role;
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

    public Optional<Reservation> reviewReservation(ReservationManager manager, String reservationId) {
        return manager.findReservation(reservationId);
    }

    public boolean approveReservation(ReservationManager manager, String reservationId) {
        return manager.updateReservationStatus(reservationId, ReservationStatus.ACCEPTED);
    }

    public boolean denyReservation(ReservationManager manager, String reservationId) {
        return manager.updateReservationStatus(reservationId, ReservationStatus.DENIED);
    }
}

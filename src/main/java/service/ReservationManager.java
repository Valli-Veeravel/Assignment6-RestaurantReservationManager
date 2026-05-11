package service;

import model.Customer;
import model.Reservation;
import model.ReservationStatus;
import model.Restaurant;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ReservationManager {
    private String managerId;
    private DataStore dataStore;
    private ValidationService validationService;
    private NotificationService notificationService;

    public ReservationManager(String managerId) {
        this(managerId, new DataStore(), new ValidationService(), new NotificationService());
    }

    public ReservationManager(
            String managerId,
            DataStore dataStore,
            ValidationService validationService,
            NotificationService notificationService
    ) {
        this.managerId = managerId;
        this.dataStore = dataStore;
        this.validationService = validationService;
        this.notificationService = notificationService;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public ValidationService getValidationService() {
        return validationService;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Optional<Reservation> createRequest(
            Customer customer,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        validationService.validateReservationRequest(customer, restaurant, dateTime, partySize);

        if (!restaurant.getAvailability(dateTime, partySize)) {
            routeToWaitlist(customer, restaurant);
            return Optional.empty();
        }

        Reservation reservation = new Reservation(
                UUID.randomUUID().toString(),
                customer,
                restaurant,
                dateTime,
                partySize,
                ReservationStatus.PENDING
        );

        restaurant.addReservation(reservation);
        customer.addReservationId(reservation.getReservationId());
        dataStore.addCustomer(customer);
        dataStore.addRestaurant(restaurant);
        dataStore.addReservation(reservation);
        notificationService.sendRestaurantNotification(restaurant, "New reservation request is pending review.");
        return Optional.of(reservation);
    }

    public boolean updateReservationStatus(String reservationId, ReservationStatus newStatus) {
        return findReservation(reservationId)
                .map(reservation -> {
                    updateReservationStatus(reservation, newStatus);
                    return true;
                })
                .orElse(false);
    }

    public void updateReservationStatus(Reservation reservation, ReservationStatus newStatus) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation is required.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Reservation status is required.");
        }

        ReservationStatus previousStatus = reservation.getStatus();

        if (newStatus == ReservationStatus.ACCEPTED && previousStatus != ReservationStatus.ACCEPTED) {
            if (!reservation.getRestaurant().getAvailability(reservation.getDateTime(), reservation.getPartySize())) {
                throw new IllegalStateException("Cannot accept reservation because capacity is no longer available.");
            }
            reservation.getRestaurant()
                    .getAvailabilitySchedule()
                    .reserveSlot(reservation.getDateTime(), reservation.getPartySize());
        }

        if (previousStatus == ReservationStatus.ACCEPTED
                && (newStatus == ReservationStatus.CANCELLED || newStatus == ReservationStatus.DENIED)) {
            reservation.getRestaurant()
                    .getAvailabilitySchedule()
                    .releaseSlot(reservation.getDateTime(), reservation.getPartySize());
        }

        reservation.updateStatus(newStatus);
        notificationService.sendCustomerNotification(
                reservation.getCustomer(),
                "Reservation status updated to " + newStatus + "."
        );
    }

    public Optional<Reservation> findReservation(String reservationId) {
        return dataStore.findReservationById(reservationId);
    }

    public void routeToWaitlist(Customer customer, Restaurant restaurant) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant is required.");
        }
        restaurant.getWaitlist().addCustomer(customer);
        dataStore.addCustomer(customer);
        dataStore.addRestaurant(restaurant);
        notificationService.sendCustomerNotification(customer, "No capacity is available. You were added to the waitlist.");
    }
}

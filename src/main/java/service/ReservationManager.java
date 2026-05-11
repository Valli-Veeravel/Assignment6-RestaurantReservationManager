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

    public boolean searchAvailability(Restaurant restaurant, LocalDateTime dateTime, int partySize) {
        validationService.validateAvailabilitySearch(restaurant, dateTime, partySize);
        return restaurant.getAvailability(dateTime, partySize);
    }

    public Optional<Reservation> createRequest(
            Customer customer,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        validationService.validateReservationRequest(customer, restaurant, dateTime, partySize);

        if (!searchAvailability(restaurant, dateTime, partySize)) {
            joinWaitlist(customer, restaurant, dateTime, partySize);
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

    public boolean acceptReservation(String reservationId) {
        return findReservation(reservationId)
                .map(reservation -> {
                    acceptReservation(reservation);
                    return true;
                })
                .orElse(false);
    }

    public void acceptReservation(Reservation reservation) {
        validateReservationForStatusChange(reservation);
        ensurePending(reservation, ReservationStatus.ACCEPTED);

        if (!searchAvailability(reservation.getRestaurant(), reservation.getDateTime(), reservation.getPartySize())) {
            throw new IllegalStateException("Cannot accept reservation because capacity is no longer available.");
        }

        reservation.getRestaurant()
                .getAvailabilitySchedule()
                .reserveSlot(reservation.getDateTime(), reservation.getPartySize());
        reservation.updateStatus(ReservationStatus.ACCEPTED);
        notificationService.sendCustomerNotification(
                reservation.getCustomer(),
                "Reservation status updated to ACCEPTED."
        );
    }

    public boolean denyReservation(String reservationId) {
        return findReservation(reservationId)
                .map(reservation -> {
                    denyReservation(reservation);
                    return true;
                })
                .orElse(false);
    }

    public void denyReservation(Reservation reservation) {
        validateReservationForStatusChange(reservation);
        ensurePending(reservation, ReservationStatus.DENIED);
        reservation.updateStatus(ReservationStatus.DENIED);
        notificationService.sendCustomerNotification(
                reservation.getCustomer(),
                "Reservation status updated to DENIED."
        );
    }

    public boolean cancelReservation(String reservationId) {
        return findReservation(reservationId)
                .map(reservation -> {
                    cancelReservation(reservation);
                    return true;
                })
                .orElse(false);
    }

    public void cancelReservation(Reservation reservation) {
        validateReservationForStatusChange(reservation);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }
        if (reservation.getStatus() == ReservationStatus.DENIED) {
            throw new IllegalStateException("Denied reservations cannot be cancelled.");
        }
        if (reservation.getStatus() == ReservationStatus.ACCEPTED) {
            reservation.getRestaurant()
                    .getAvailabilitySchedule()
                    .releaseSlot(reservation.getDateTime(), reservation.getPartySize());
        }

        reservation.updateStatus(ReservationStatus.CANCELLED);
        reservation.getCustomer().removeReservationId(reservation.getReservationId());
        notificationService.sendCustomerNotification(
                reservation.getCustomer(),
                "Reservation status updated to CANCELLED."
        );
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

        switch (newStatus) {
            case PENDING -> {
                if (reservation.getStatus() != ReservationStatus.PENDING) {
                    throw new IllegalStateException("Only new reservations can be pending.");
                }
            }
            case ACCEPTED -> acceptReservation(reservation);
            case DENIED -> denyReservation(reservation);
            case CANCELLED -> cancelReservation(reservation);
        }
    }

    public Optional<Reservation> findReservation(String reservationId) {
        return dataStore.findReservationById(reservationId);
    }

    public boolean joinWaitlist(Customer customer, Restaurant restaurant, LocalDateTime dateTime, int partySize) {
        validationService.validateReservationRequest(customer, restaurant, dateTime, partySize);
        if (searchAvailability(restaurant, dateTime, partySize)) {
            return false;
        }

        routeToWaitlist(customer, restaurant);
        return true;
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

    private void validateReservationForStatusChange(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation is required.");
        }
        validationService.validateReservationRequest(
                reservation.getCustomer(),
                reservation.getRestaurant(),
                reservation.getDateTime(),
                reservation.getPartySize()
        );
    }

    private void ensurePending(Reservation reservation, ReservationStatus targetStatus) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be marked " + targetStatus + ".");
        }
    }
}

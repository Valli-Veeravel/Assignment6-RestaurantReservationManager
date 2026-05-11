package service;

import model.Customer;
import model.CustomerNotification;
import model.Reservation;
import model.ReservationStatus;
import model.Restaurant;
import model.Review;
import model.Staff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business service for availability and reservation status workflows.
 */
public class ReservationManager {
    private String managerId;
    private DataStore dataStore;
    private ValidationService validationService;
    private NotificationService notificationService;
    private ReviewService reviewService;
    private WaitlistService waitlistService;

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
        refreshSupportingServices();
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
        refreshSupportingServices();
    }

    public ValidationService getValidationService() {
        return validationService;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
        refreshSupportingServices();
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
        refreshSupportingServices();
    }

    public boolean searchAvailability(Restaurant restaurant, LocalDateTime dateTime, int partySize) {
        validationService.validateAvailabilitySearch(restaurant, dateTime, partySize);
        return restaurant.getAvailability(dateTime, partySize);
    }

    public Optional<Customer> authenticateCustomer(String email, String password) {
        return dataStore.findCustomerByEmail(normalizeEmail(email))
                .filter(customer -> customer.passwordMatches(password));
    }

    public Optional<Staff> authenticateStaff(String email, String password) {
        return dataStore.findStaffByEmail(normalizeEmail(email))
                .filter(staff -> staff.passwordMatches(password));
    }

    public List<Restaurant> getRestaurants() {
        return new ArrayList<>(dataStore.getAllRestaurants());
    }

    public List<Customer> getCustomers() {
        return new ArrayList<>(dataStore.getAllCustomers());
    }

    public List<Staff> getStaffMembers() {
        return new ArrayList<>(dataStore.getAllStaffMembers());
    }

    public List<WaitlistRecord> getWaitlistRecords() {
        return waitlistService.getWaitlistRecords();
    }

    public List<WaitlistRecord> getWaitlistRecordsForStaff(Staff staff) {
        return waitlistService.getWaitlistRecordsForStaff(staff);
    }

    public List<Reservation> getReservationsForCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        return dataStore.getAllReservations().stream()
                .filter(reservation -> reservation.getCustomer() != null)
                .filter(reservation -> customer.getCustomerId().equals(reservation.getCustomer().getCustomerId()))
                .filter(this::isVisibleCustomerReservation)
                .sorted(Comparator.comparing(Reservation::getDateTime))
                .toList();
    }

    public List<CustomerNotification> getNotificationsForCustomer(Customer customer) {
        return notificationService.getNotificationsForCustomer(customer);
    }

    public List<Reservation> getPendingReservations() {
        return dataStore.getAllReservations().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                .sorted(Comparator.comparing(Reservation::getDateTime))
                .toList();
    }

    public List<Reservation> getPendingReservationsForStaff(Staff staff) {
        return getReservationsForStaffRestaurant(staff, ReservationStatus.PENDING);
    }

    public List<Reservation> getCurrentReservationsForStaff(Staff staff) {
        return getReservationsForStaffRestaurant(staff, ReservationStatus.ACCEPTED);
    }

    public List<Review> getReviews() {
        return reviewService.getReviews();
    }

    public List<Review> getReviewsForRestaurant(Restaurant restaurant) {
        return reviewService.getReviewsForRestaurant(restaurant);
    }

    public Optional<Reservation> createRequest(
            Customer customer,
            Restaurant restaurant,
            LocalDateTime dateTime,
            int partySize
    ) {
        validationService.validateReservationRequest(customer, restaurant, dateTime, partySize);
        if (hasActiveReservationAt(customer, dateTime)) {
            throw new IllegalStateException("Customer already has an active reservation for this date and time.");
        }

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
        notificationService.sendCustomerNotification(
                customer,
                "Your reservation request at " + restaurant.getName() + " is PENDING."
        );
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

    public boolean acceptReservation(Staff staff, String reservationId) {
        return findReservation(reservationId)
                .map(reservation -> {
                    validateStaffCanManageReservation(staff, reservation);
                    acceptReservation(reservation);
                    return true;
                })
                .orElse(false);
    }

    public void acceptReservation(Reservation reservation) {
        validateReservationForStatusChange(reservation);
        ensurePending(reservation, ReservationStatus.ACCEPTED);

        // Availability is checked again at approval time to avoid accepting stale requests.
        if (!searchAvailability(reservation.getRestaurant(), reservation.getDateTime(), reservation.getPartySize())) {
            throw new IllegalStateException("Cannot accept reservation because capacity is no longer available.");
        }

        reservation.getRestaurant()
                .getAvailabilitySchedule()
                .reserveSlot(reservation.getDateTime(), reservation.getPartySize());
        reservation.updateStatus(ReservationStatus.ACCEPTED);
        notificationService.sendCustomerNotification(
                reservation.getCustomer(),
                "Your reservation request has been Accepted."
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

    public boolean denyReservation(Staff staff, String reservationId) {
        return findReservation(reservationId)
                .map(reservation -> {
                    validateStaffCanManageReservation(staff, reservation);
                    denyReservation(reservation);
                    return true;
                })
                .orElse(false);
    }

    public void denyReservation(Reservation reservation) {
        validateReservationForStatusChange(reservation);
        ensurePending(reservation, ReservationStatus.DENIED);
        reservation.updateStatus(ReservationStatus.DENIED);
        reservation.getCustomer().removeReservationId(reservation.getReservationId());
        notificationService.sendCustomerNotification(
                reservation.getCustomer(),
                "Your reservation request has been Denied."
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
        boolean wasAccepted = reservation.getStatus() == ReservationStatus.ACCEPTED;

        // Only accepted reservations have consumed capacity, so only they release seats.
        if (wasAccepted) {
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

        if (wasAccepted) {
            promoteNextWaitlistEntry(reservation.getRestaurant(), reservation.getDateTime());
        }
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

        // Keep legacy callers routed through the same explicit transition methods.
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

    public Review submitReview(Customer customer, Restaurant restaurant, int rating, String comment) {
        return reviewService.submitReview(customer, restaurant, rating, comment);
    }

    public boolean joinWaitlist(Customer customer, Restaurant restaurant, LocalDateTime dateTime, int partySize) {
        return waitlistService.joinWaitlist(customer, restaurant, dateTime, partySize);
    }

    public Optional<Reservation> promoteNextWaitlistEntry(Restaurant restaurant, LocalDateTime dateTime) {
        return waitlistService.promoteNextWaitlistEntry(restaurant, dateTime);
    }

    public void routeToWaitlist(Customer customer, Restaurant restaurant) {
        waitlistService.routeToWaitlist(customer, restaurant);
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

    private boolean isVisibleCustomerReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.PENDING
                || reservation.getStatus() == ReservationStatus.ACCEPTED;
    }

    private boolean hasActiveReservationAt(Customer customer, LocalDateTime dateTime) {
        return dataStore.getAllReservations().stream()
                .filter(reservation -> reservation.getCustomer() != null)
                .filter(reservation -> customer.getCustomerId().equals(reservation.getCustomer().getCustomerId()))
                .filter(this::isVisibleCustomerReservation)
                .anyMatch(reservation -> reservation.getDateTime().equals(dateTime));
    }

    private List<Reservation> getReservationsForStaffRestaurant(Staff staff, ReservationStatus status) {
        return dataStore.getAllReservations().stream()
                .filter(reservation -> reservation.getStatus() == status)
                .filter(reservation -> restaurantMatchesStaff(staff, reservation.getRestaurant()))
                .sorted(Comparator.comparing(Reservation::getDateTime))
                .toList();
    }

    private boolean restaurantMatchesStaff(Staff staff, Restaurant restaurant) {
        return staff != null
                && staff.getRestaurant() != null
                && restaurant != null
                && staff.getRestaurant().getRestaurantId().equals(restaurant.getRestaurantId());
    }

    private void validateStaffCanManageReservation(Staff staff, Reservation reservation) {
        if (staff == null) {
            throw new IllegalArgumentException("Staff is required.");
        }
        if (staff.getRestaurant() != null && !restaurantMatchesStaff(staff, reservation.getRestaurant())) {
            throw new IllegalStateException("Staff can only manage reservations for their assigned restaurant.");
        }
    }

    private void refreshSupportingServices() {
        this.reviewService = new ReviewService(dataStore, validationService);
        this.waitlistService = new WaitlistService(dataStore, validationService, notificationService);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }
}

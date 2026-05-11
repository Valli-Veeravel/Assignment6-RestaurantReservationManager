package service;

import model.Customer;
import model.CustomerNotification;
import model.Reservation;
import model.ReservationStatus;
import model.Restaurant;
import model.Review;
import model.Staff;
import model.WaitlistEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business service for availability, reservations, waitlists, and reviews.
 */
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
        return dataStore.getAllRestaurants().stream()
                .flatMap(restaurant -> restaurant.getWaitlist().listEntries().stream()
                        .map(entry -> new WaitlistRecord(restaurant, entry)))
                .sorted(Comparator
                        .comparing(
                                (WaitlistRecord record) -> record.entry().getRequestedDateTime(),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                record -> record.entry().getJoinedAt(),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                .toList();
    }

    public List<WaitlistRecord> getWaitlistRecordsForStaff(Staff staff) {
        return getWaitlistRecords().stream()
                .filter(record -> restaurantMatchesStaff(staff, record.restaurant()))
                .toList();
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
        validationService.validateReview(customer, restaurant, rating, comment);
        Review review = new Review(UUID.randomUUID().toString(), customer, restaurant, rating, comment.trim());
        restaurant.addReview(review);
        dataStore.addReview(review);
        return review;
    }

    public boolean joinWaitlist(Customer customer, Restaurant restaurant, LocalDateTime dateTime, int partySize) {
        validationService.validateReservationRequest(customer, restaurant, dateTime, partySize);
        if (searchAvailability(restaurant, dateTime, partySize)) {
            return false;
        }

        WaitlistEntry entry = new WaitlistEntry(
                UUID.randomUUID().toString(),
                customer,
                dateTime,
                partySize,
                LocalDateTime.now()
        );
        restaurant.getWaitlist().addEntry(entry);
        dataStore.addCustomer(customer);
        dataStore.addRestaurant(restaurant);
        notificationService.sendCustomerNotification(customer, "No capacity is available. You were added to the waitlist.");
        return true;
    }

    public Optional<Reservation> promoteNextWaitlistEntry(Restaurant restaurant, LocalDateTime dateTime) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant is required.");
        }
        if (dateTime == null) {
            throw new IllegalArgumentException("Reservation date and time is required.");
        }

        Optional<WaitlistEntry> nextEntry = restaurant.getWaitlist().listEntries().stream()
                .filter(entry -> dateTime.equals(entry.getRequestedDateTime()))
                .findFirst();

        if (nextEntry.isEmpty()) {
            return Optional.empty();
        }

        WaitlistEntry entry = nextEntry.get();
        if (!restaurant.getAvailability(dateTime, entry.getPartySize())) {
            return Optional.empty();
        }
        if (hasActiveReservationAt(entry.getCustomer(), dateTime)) {
            return Optional.empty();
        }

        Reservation reservation = new Reservation(
                UUID.randomUUID().toString(),
                entry.getCustomer(),
                restaurant,
                entry.getRequestedDateTime(),
                entry.getPartySize(),
                ReservationStatus.PENDING
        );

        restaurant.addReservation(reservation);
        entry.getCustomer().addReservationId(reservation.getReservationId());
        restaurant.getWaitlist().removeEntry(entry);
        dataStore.addCustomer(entry.getCustomer());
        dataStore.addRestaurant(restaurant);
        dataStore.addReservation(reservation);
        notificationService.sendCustomerNotification(
                entry.getCustomer(),
                "A table opened up at " + restaurant.getName() + ". Your waitlist request is now PENDING."
        );
        notificationService.sendRestaurantNotification(
                restaurant,
                "A waitlisted customer was promoted to a pending reservation."
        );
        return Optional.of(reservation);
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

    public record WaitlistRecord(Restaurant restaurant, WaitlistEntry entry) {
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

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }
}

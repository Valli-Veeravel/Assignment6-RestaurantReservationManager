package service;

import model.Customer;
import model.Reservation;
import model.ReservationStatus;
import model.Restaurant;
import model.Staff;
import model.WaitlistEntry;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WaitlistService {
    private final DataStore dataStore;
    private final ValidationService validationService;
    private final NotificationService notificationService;

    public WaitlistService(
            DataStore dataStore,
            ValidationService validationService,
            NotificationService notificationService
    ) {
        this.dataStore = dataStore;
        this.validationService = validationService;
        this.notificationService = notificationService;
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

    public boolean joinWaitlist(Customer customer, Restaurant restaurant, LocalDateTime dateTime, int partySize) {
        validationService.validateReservationRequest(customer, restaurant, dateTime, partySize);
        if (restaurant.getAvailability(dateTime, partySize)) {
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

    private boolean hasActiveReservationAt(Customer customer, LocalDateTime dateTime) {
        return dataStore.getAllReservations().stream()
                .filter(reservation -> reservation.getCustomer() != null)
                .filter(reservation -> customer.getCustomerId().equals(reservation.getCustomer().getCustomerId()))
                .filter(this::isVisibleCustomerReservation)
                .anyMatch(reservation -> reservation.getDateTime().equals(dateTime));
    }

    private boolean isVisibleCustomerReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.PENDING
                || reservation.getStatus() == ReservationStatus.ACCEPTED;
    }

    private boolean restaurantMatchesStaff(Staff staff, Restaurant restaurant) {
        return staff != null
                && staff.getRestaurant() != null
                && restaurant != null
                && staff.getRestaurant().getRestaurantId().equals(restaurant.getRestaurantId());
    }
}

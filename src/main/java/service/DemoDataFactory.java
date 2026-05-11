package service;

import model.Customer;
import model.Restaurant;
import model.Staff;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Builds an in-memory dataset so the JavaFX app can be tested without file or database setup.
 */
public final class DemoDataFactory {
    private DemoDataFactory() {
    }

    public static ReservationManager createDemoManager() {
        DataStore dataStore = new DataStore();
        ReservationManager manager = new ReservationManager(
                "manager-demo",
                dataStore,
                new ValidationService(),
                new NotificationService()
        );

        Restaurant bistro = new Restaurant("restaurant-1", "Downtown Bistro", "101 Market Street", 40);
        Restaurant garden = new Restaurant("restaurant-2", "Garden Table", "22 Willow Avenue", 28);
        Restaurant harbor = new Restaurant("restaurant-3", "Harbor Grill", "8 Pier Road", 50);
        List<Restaurant> restaurants = List.of(bistro, garden, harbor);
        restaurants.forEach(dataStore::addRestaurant);
        seedCapacity(restaurants);

        Customer ava = new Customer("user-1", "customer-1", "Ava Customer", "ava@example.com", "555-0100", "ava123");
        Customer ben = new Customer("user-2", "customer-2", "Ben Customer", "ben@example.com", "555-0101", "ben123");
        dataStore.addCustomer(ava);
        dataStore.addCustomer(ben);

        Staff sam = new Staff("user-3", "staff-1", "Sam Staff", "sam@example.com", "555-0102", "Server", bistro, "sam123");
        Staff mia = new Staff("user-4", "staff-2", "Mia Manager", "mia@example.com", "555-0103", "Manager", garden, "mia123");
        Staff leo = new Staff("user-5", "staff-3", "Leo Harbor", "leo@example.com", "555-0104", "Server", harbor, "leo123");
        dataStore.addStaff(sam);
        dataStore.addStaff(mia);
        dataStore.addStaff(leo);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        manager.createRequest(ava, bistro, LocalDateTime.of(tomorrow, LocalTime.of(18, 0)), 2);
        manager.createRequest(ben, bistro, LocalDateTime.of(tomorrow, LocalTime.of(19, 0)), 2)
                .ifPresent(manager::acceptReservation);
        manager.createRequest(ben, garden, LocalDateTime.of(tomorrow.plusDays(1), LocalTime.of(19, 0)), 4);
        LocalDateTime waitlistSlot = LocalDateTime.of(tomorrow.plusDays(2), LocalTime.of(20, 0));
        harbor.getAvailabilitySchedule().setCapacity(waitlistSlot, 0);
        manager.joinWaitlist(ava, harbor, waitlistSlot, 2);
        manager.submitReview(ava, bistro, 5, "Friendly staff and a relaxed dinner service.");
        manager.submitReview(ben, harbor, 4, "Great seafood and easy reservation flow.");

        return manager;
    }

    private static void seedCapacity(List<Restaurant> restaurants) {
        // Seed common dinner slots for the next week so availability searches have useful results.
        List<LocalTime> times = List.of(
                LocalTime.of(17, 0),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                LocalTime.of(21, 0)
        );

        for (Restaurant restaurant : restaurants) {
            for (int day = 1; day <= 7; day++) {
                for (LocalTime time : times) {
                    LocalDateTime slot = LocalDateTime.of(LocalDate.now().plusDays(day), time);
                    restaurant.getAvailabilitySchedule().setCapacity(slot, restaurant.getMaxCapacity());
                }
            }
        }
    }
}

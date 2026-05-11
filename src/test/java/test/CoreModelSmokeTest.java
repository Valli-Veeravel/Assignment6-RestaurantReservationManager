package test;

import model.AvailabilitySchedule;
import model.Customer;
import model.Reservation;
import model.ReservationStatus;
import model.Restaurant;
import org.junit.jupiter.api.Test;
import service.ReservationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreModelSmokeTest {
    @Test
    void availabilityScheduleReservesAndReleasesCapacity() {
        AvailabilitySchedule schedule = new AvailabilitySchedule("schedule-1");
        LocalDateTime slot = LocalDateTime.now().plusDays(1);

        schedule.setCapacity(slot, 4);

        assertTrue(schedule.hasCapacity(slot, 2));
        schedule.reserveSlot(slot, 2);
        assertEquals(2, schedule.getCapacity(slot));
        assertFalse(schedule.hasCapacity(slot, 3));

        schedule.releaseSlot(slot, 2);
        assertEquals(4, schedule.getCapacity(slot));
    }

    @Test
    void reservationManagerCreatesPendingReservationWhenCapacityExists() {
        Restaurant restaurant = new Restaurant("restaurant-1", "Test Bistro", "1 Main St", 40);
        Customer customer = new Customer("user-1", "customer-1", "Ava Customer", "ava@example.com", "555-0100");
        ReservationManager manager = new ReservationManager("manager-1");
        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        restaurant.getAvailabilitySchedule().setCapacity(slot, 4);

        Optional<Reservation> result = manager.createRequest(customer, restaurant, slot, 2);

        assertTrue(result.isPresent());
        assertEquals(ReservationStatus.PENDING, result.get().getStatus());
        assertTrue(customer.getReservationIds().contains(result.get().getReservationId()));
        assertTrue(restaurant.getReservations().contains(result.get()));
    }

    @Test
    void reservationManagerRoutesCustomerToWaitlistWhenCapacityIsUnavailable() {
        Restaurant restaurant = new Restaurant("restaurant-1", "Test Bistro", "1 Main St", 40);
        Customer customer = new Customer("user-1", "customer-1", "Ava Customer", "ava@example.com", "555-0100");
        ReservationManager manager = new ReservationManager("manager-1");
        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        restaurant.getAvailabilitySchedule().setCapacity(slot, 1);

        Optional<Reservation> result = manager.createRequest(customer, restaurant, slot, 2);

        assertTrue(result.isEmpty());
        assertEquals(1, restaurant.getWaitlist().size());
        assertEquals(customer, restaurant.getWaitlist().peekNextCustomer().orElseThrow());
    }

    @Test
    void reservationStatusUpdatesReserveAndReleaseCapacity() {
        Restaurant restaurant = new Restaurant("restaurant-1", "Test Bistro", "1 Main St", 40);
        Customer customer = new Customer("user-1", "customer-1", "Ava Customer", "ava@example.com", "555-0100");
        ReservationManager manager = new ReservationManager("manager-1");
        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        restaurant.getAvailabilitySchedule().setCapacity(slot, 4);
        Reservation reservation = manager.createRequest(customer, restaurant, slot, 2).orElseThrow();

        manager.updateReservationStatus(reservation, ReservationStatus.ACCEPTED);

        assertEquals(ReservationStatus.ACCEPTED, reservation.getStatus());
        assertEquals(2, restaurant.getAvailabilitySchedule().getCapacity(slot));

        manager.updateReservationStatus(reservation, ReservationStatus.CANCELLED);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(4, restaurant.getAvailabilitySchedule().getCapacity(slot));
    }
}

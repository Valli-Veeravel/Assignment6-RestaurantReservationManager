package test;

import model.AvailabilitySchedule;
import model.Customer;
import model.Reservation;
import model.ReservationStatus;
import model.Restaurant;
import model.Staff;
import org.junit.jupiter.api.Test;
import service.ReservationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationManagerTest {
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
    void reservationManagerSearchesAvailabilityWithValidation() {
        Restaurant restaurant = new Restaurant("restaurant-1", "Test Bistro", "1 Main St", 4);
        ReservationManager manager = new ReservationManager("manager-1");
        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        restaurant.getAvailabilitySchedule().setCapacity(slot, 3);

        assertTrue(manager.searchAvailability(restaurant, slot, 2));
        assertFalse(manager.searchAvailability(restaurant, slot, 4));
        assertThrows(IllegalArgumentException.class, () -> manager.searchAvailability(restaurant, slot, 0));
        assertThrows(IllegalArgumentException.class, () -> manager.searchAvailability(restaurant, slot, 5));
        assertThrows(IllegalArgumentException.class, () -> manager.searchAvailability(null, slot, 2));
        assertThrows(IllegalArgumentException.class, () -> manager.searchAvailability(restaurant, LocalDateTime.now().minusDays(1), 2));
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

    @Test
    void reservationRequestRejectsInvalidInput() {
        Restaurant restaurant = new Restaurant("restaurant-1", "Test Bistro", "1 Main St", 4);
        Customer customer = new Customer("user-1", "customer-1", "Ava Customer", "ava@example.com", "555-0100");
        ReservationManager manager = new ReservationManager("manager-1");
        LocalDateTime futureSlot = LocalDateTime.now().plusDays(1);
        LocalDateTime pastSlot = LocalDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> manager.createRequest(null, restaurant, futureSlot, 2));
        assertThrows(IllegalArgumentException.class, () -> manager.createRequest(customer, null, futureSlot, 2));
        assertThrows(IllegalArgumentException.class, () -> manager.createRequest(customer, restaurant, pastSlot, 2));
        assertThrows(IllegalArgumentException.class, () -> manager.createRequest(customer, restaurant, futureSlot, 0));
        assertThrows(IllegalArgumentException.class, () -> manager.createRequest(customer, restaurant, futureSlot, 5));
    }

    @Test
    void acceptingReservationChecksAvailabilityAgain() {
        Restaurant restaurant = new Restaurant("restaurant-1", "Test Bistro", "1 Main St", 40);
        Customer firstCustomer = new Customer("user-1", "customer-1", "Ava Customer", "ava@example.com", "555-0100");
        Customer secondCustomer = new Customer("user-2", "customer-2", "Ben Customer", "ben@example.com", "555-0101");
        ReservationManager manager = new ReservationManager("manager-1");
        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        restaurant.getAvailabilitySchedule().setCapacity(slot, 4);
        Reservation firstReservation = manager.createRequest(firstCustomer, restaurant, slot, 3).orElseThrow();
        Reservation secondReservation = manager.createRequest(secondCustomer, restaurant, slot, 3).orElseThrow();

        manager.acceptReservation(firstReservation);

        assertEquals(1, restaurant.getAvailabilitySchedule().getCapacity(slot));
        assertThrows(IllegalStateException.class, () -> manager.acceptReservation(secondReservation));
        assertEquals(ReservationStatus.PENDING, secondReservation.getStatus());
    }

    @Test
    void staffAcceptsAndDeniesPendingReservationsOnly() {
        Restaurant restaurant = new Restaurant("restaurant-1", "Test Bistro", "1 Main St", 40);
        Customer firstCustomer = new Customer("user-1", "customer-1", "Ava Customer", "ava@example.com", "555-0100");
        Customer secondCustomer = new Customer("user-2", "customer-2", "Ben Customer", "ben@example.com", "555-0101");
        Staff staff = new Staff("staff-user-1", "staff-1", "Sam Staff", "sam@example.com", "555-0102", "Host");
        ReservationManager manager = new ReservationManager("manager-1");
        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        restaurant.getAvailabilitySchedule().setCapacity(slot, 4);
        Reservation acceptedReservation = manager.createRequest(firstCustomer, restaurant, slot, 2).orElseThrow();
        Reservation deniedReservation = manager.createRequest(secondCustomer, restaurant, slot, 1).orElseThrow();

        assertTrue(staff.approveReservation(manager, acceptedReservation.getReservationId()));
        assertEquals(ReservationStatus.ACCEPTED, acceptedReservation.getStatus());
        assertEquals(2, restaurant.getAvailabilitySchedule().getCapacity(slot));

        assertTrue(staff.denyReservation(manager, deniedReservation.getReservationId()));
        assertEquals(ReservationStatus.DENIED, deniedReservation.getStatus());
        assertEquals(2, restaurant.getAvailabilitySchedule().getCapacity(slot));
        assertThrows(IllegalStateException.class, () -> staff.denyReservation(manager, acceptedReservation.getReservationId()));
    }
}

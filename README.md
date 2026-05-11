# Restaurant Reservation Manager

Initial Maven JavaFX project skeleton for the CS151 Assignment 6 Restaurant Reservation Manager.

This version focuses on the domain model and core service layer only. The JavaFX UI will be added later after the backend classes are stable.

## Project Layout

- `src/main/java/model` - domain classes such as users, restaurants, reservations, schedules, waitlists, and reviews
- `src/main/java/service` - core service skeletons for reservation management, validation, notifications, and in-memory storage
- `src/main/java/controller` - placeholder package for future JavaFX controllers
- `src/main/java/view` - placeholder package for future JavaFX views/resources
- `src/test/java/test` - JUnit 5 smoke tests

## Build

```bash
mvn clean compile
```

## Test

```bash
mvn test
```

## Run

No JavaFX application entry point exists yet because the UI is intentionally out of scope for this first skeleton. JavaFX run instructions will be added when the UI is implemented.

## Notes

- The project currently uses in-memory storage through `DataStore`.
- `ReservationManager` creates pending reservation requests, routes unavailable requests to the restaurant waitlist, and updates reservation status.
- `AvailabilitySchedule` tracks remaining capacity per date/time slot.

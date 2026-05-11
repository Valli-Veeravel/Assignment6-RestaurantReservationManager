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

```bash
mvn javafx:run
```

The app opens with a login / role selection screen. Use one of the seeded demo accounts to test the reservation, waitlist, approval, cancellation, and review workflows.

## Demo Logins

### Customers

| Name | Email | Password |
| --- | --- | --- |
| Ava Customer | `ava@example.com` | `ava123` |
| Ben Customer | `ben@example.com` | `ben123` |

### Staff

| Name | Restaurant | Position | Email | Password |
| --- | --- | --- | --- | --- |
| Sam Staff | Downtown Bistro | Server | `sam@example.com` | `sam123` |
| Mia Manager | Garden Table | Manager | `mia@example.com` | `mia123` |
| Leo Harbor | Harbor Grill | Server | `leo@example.com` | `leo123` |

## Notes

- The project currently uses in-memory storage through `DataStore`.
- `ReservationManager` creates pending reservation requests, routes unavailable requests to the restaurant waitlist, and updates reservation status.
- `AvailabilitySchedule` tracks remaining capacity per date/time slot.

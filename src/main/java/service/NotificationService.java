package service;

import model.Customer;
import model.CustomerNotification;
import model.Restaurant;
import model.Staff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NotificationService {
    private final Map<String, List<CustomerNotification>> customerNotifications;

    public NotificationService() {
        this.customerNotifications = new LinkedHashMap<>();
    }

    public void sendCustomerNotification(Customer customer, String message) {
        if (customer != null && message != null) {
            CustomerNotification notification = new CustomerNotification(
                    UUID.randomUUID().toString(),
                    customer,
                    message,
                    LocalDateTime.now()
            );
            customerNotifications
                    .computeIfAbsent(customer.getCustomerId(), customerId -> new ArrayList<>())
                    .add(notification);
            System.out.println("Customer notification for " + customer.getName() + ": " + message);
        }
    }

    public List<CustomerNotification> getNotificationsForCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        return customerNotifications
                .getOrDefault(customer.getCustomerId(), List.of())
                .stream()
                .sorted(Comparator.comparing(CustomerNotification::getCreatedAt).reversed())
                .toList();
    }

    public void sendStaffNotification(Staff staff, String message) {
        if (staff != null && message != null) {
            System.out.println("Staff notification for " + staff.getName() + ": " + message);
        }
    }

    public void sendRestaurantNotification(Restaurant restaurant, String message) {
        if (restaurant != null && message != null) {
            System.out.println("Restaurant notification for " + restaurant.getName() + ": " + message);
        }
    }
}

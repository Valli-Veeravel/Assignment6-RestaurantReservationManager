package service;

import model.Customer;
import model.Restaurant;
import model.Staff;

public class NotificationService {
    public void sendCustomerNotification(Customer customer, String message) {
        if (customer != null && message != null) {
            System.out.println("Customer notification for " + customer.getName() + ": " + message);
        }
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

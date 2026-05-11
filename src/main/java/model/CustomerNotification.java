package model;

import java.time.LocalDateTime;

public class CustomerNotification {
    private String notificationId;
    private Customer customer;
    private String message;
    private LocalDateTime createdAt;
    private boolean read;

    public CustomerNotification(String notificationId, Customer customer, String message, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.customer = customer;
        this.message = message;
        this.createdAt = createdAt;
        this.read = false;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}

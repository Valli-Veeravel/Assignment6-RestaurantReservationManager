package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class Waitlist {
    private String waitlistId;
    private Queue<Customer> queuedCustomers;

    public Waitlist(String waitlistId) {
        this.waitlistId = waitlistId;
        this.queuedCustomers = new LinkedList<>();
    }

    public String getWaitlistId() {
        return waitlistId;
    }

    public void setWaitlistId(String waitlistId) {
        this.waitlistId = waitlistId;
    }

    public Queue<Customer> getQueuedCustomers() {
        return new LinkedList<>(queuedCustomers);
    }

    public void setQueuedCustomers(Queue<Customer> queuedCustomers) {
        this.queuedCustomers = new LinkedList<>(queuedCustomers);
    }

    public void addCustomer(Customer customer) {
        if (customer != null && !queuedCustomers.contains(customer)) {
            queuedCustomers.add(customer);
        }
    }

    public boolean removeCustomer(Customer customer) {
        return queuedCustomers.remove(customer);
    }

    public Optional<Customer> peekNextCustomer() {
        return Optional.ofNullable(queuedCustomers.peek());
    }

    public List<Customer> listCustomers() {
        return new ArrayList<>(queuedCustomers);
    }

    public int size() {
        return queuedCustomers.size();
    }

    public void notifyAvailability() {
        peekNextCustomer().ifPresent(customer ->
                System.out.println("Availability notification ready for " + customer.getName()));
    }
}

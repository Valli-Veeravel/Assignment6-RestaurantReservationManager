package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

public class Waitlist {
    private String waitlistId;
    private Queue<WaitlistEntry> queuedEntries;

    public Waitlist(String waitlistId) {
        this.waitlistId = waitlistId;
        this.queuedEntries = new LinkedList<>();
    }

    public String getWaitlistId() {
        return waitlistId;
    }

    public void setWaitlistId(String waitlistId) {
        this.waitlistId = waitlistId;
    }

    public Queue<Customer> getQueuedCustomers() {
        return queuedEntries.stream()
                .map(WaitlistEntry::getCustomer)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public void setQueuedCustomers(Queue<Customer> queuedCustomers) {
        this.queuedEntries = queuedCustomers.stream()
                .map(customer -> new WaitlistEntry(
                        waitlistId + "-" + customer.getCustomerId(),
                        customer,
                        null,
                        0,
                        null
                ))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public Queue<WaitlistEntry> getQueuedEntries() {
        return new LinkedList<>(queuedEntries);
    }

    public void setQueuedEntries(Queue<WaitlistEntry> queuedEntries) {
        this.queuedEntries = new LinkedList<>(queuedEntries);
    }

    public void addCustomer(Customer customer) {
        if (customer != null && listCustomers().stream().noneMatch(customer::equals)) {
            queuedEntries.add(new WaitlistEntry(
                    waitlistId + "-" + customer.getCustomerId(),
                    customer,
                    null,
                    0,
                    null
            ));
        }
    }

    public void addEntry(WaitlistEntry entry) {
        if (entry != null && !containsMatchingEntry(entry)) {
            queuedEntries.add(entry);
        }
    }

    public boolean removeEntry(WaitlistEntry entry) {
        return queuedEntries.remove(entry);
    }

    public boolean removeCustomer(Customer customer) {
        return queuedEntries.removeIf(entry -> entry.getCustomer().equals(customer));
    }

    public Optional<Customer> peekNextCustomer() {
        return Optional.ofNullable(queuedEntries.peek()).map(WaitlistEntry::getCustomer);
    }

    public List<Customer> listCustomers() {
        return queuedEntries.stream()
                .map(WaitlistEntry::getCustomer)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<WaitlistEntry> listEntries() {
        return new ArrayList<>(queuedEntries);
    }

    public int size() {
        return queuedEntries.size();
    }

    public void notifyAvailability() {
        peekNextCustomer().ifPresent(customer ->
                System.out.println("Availability notification ready for " + customer.getName()));
    }

    private boolean containsMatchingEntry(WaitlistEntry newEntry) {
        return queuedEntries.stream().anyMatch(existing ->
                existing.getCustomer().equals(newEntry.getCustomer())
                        && Objects.equals(existing.getRequestedDateTime(), newEntry.getRequestedDateTime())
                        && existing.getPartySize() == newEntry.getPartySize());
    }
}

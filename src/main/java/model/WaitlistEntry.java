package model;

import java.time.LocalDateTime;

public class WaitlistEntry {
    private String waitlistEntryId;
    private Customer customer;
    private LocalDateTime requestedDateTime;
    private int partySize;
    private LocalDateTime joinedAt;

    public WaitlistEntry(
            String waitlistEntryId,
            Customer customer,
            LocalDateTime requestedDateTime,
            int partySize,
            LocalDateTime joinedAt
    ) {
        this.waitlistEntryId = waitlistEntryId;
        this.customer = customer;
        this.requestedDateTime = requestedDateTime;
        this.partySize = partySize;
        this.joinedAt = joinedAt;
    }

    public String getWaitlistEntryId() {
        return waitlistEntryId;
    }

    public void setWaitlistEntryId(String waitlistEntryId) {
        this.waitlistEntryId = waitlistEntryId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDateTime getRequestedDateTime() {
        return requestedDateTime;
    }

    public void setRequestedDateTime(LocalDateTime requestedDateTime) {
        this.requestedDateTime = requestedDateTime;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}

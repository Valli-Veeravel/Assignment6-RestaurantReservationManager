package model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AvailabilitySchedule {
    private String scheduleId;
    private Map<LocalDateTime, Integer> capacityBySlot;

    public AvailabilitySchedule(String scheduleId) {
        this.scheduleId = scheduleId;
        this.capacityBySlot = new HashMap<>();
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Map<LocalDateTime, Integer> getCapacityBySlot() {
        return Collections.unmodifiableMap(capacityBySlot);
    }

    public void setCapacityBySlot(Map<LocalDateTime, Integer> capacityBySlot) {
        this.capacityBySlot = new HashMap<>(capacityBySlot);
    }

    public void setCapacity(LocalDateTime dateTime, int capacity) {
        Objects.requireNonNull(dateTime, "dateTime cannot be null");
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative.");
        }
        capacityBySlot.put(dateTime, capacity);
    }

    public int getCapacity(LocalDateTime dateTime) {
        return capacityBySlot.getOrDefault(dateTime, 0);
    }

    public boolean hasCapacity(LocalDateTime dateTime, int partySize) {
        return dateTime != null && partySize > 0 && getCapacity(dateTime) >= partySize;
    }

    public void reserveSlot(LocalDateTime dateTime, int partySize) {
        if (!hasCapacity(dateTime, partySize)) {
            throw new IllegalArgumentException("Not enough capacity for the requested slot.");
        }
        capacityBySlot.put(dateTime, getCapacity(dateTime) - partySize);
    }

    public void releaseSlot(LocalDateTime dateTime, int partySize) {
        Objects.requireNonNull(dateTime, "dateTime cannot be null");
        if (partySize <= 0) {
            throw new IllegalArgumentException("Party size must be positive.");
        }
        capacityBySlot.put(dateTime, getCapacity(dateTime) + partySize);
    }
}

package service;

import model.Restaurant;
import model.WaitlistEntry;

public record WaitlistRecord(Restaurant restaurant, WaitlistEntry entry) {
}

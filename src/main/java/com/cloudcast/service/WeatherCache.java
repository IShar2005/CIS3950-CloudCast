package com.cloudcast.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache with a per-entry TTL.
 * Used to avoid repeatedly hitting the OpenWeatherMap free-tier rate limit.
 * Default TTL is 5 minutes.
 */
@Component
public class WeatherCache {

    private static final Duration TTL = Duration.ofMinutes(5);

    private record Entry(Object value, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            return null;
        }
        return (T) entry.value();
    }

    public void put(String key, Object value) {
        store.put(key, new Entry(value, Instant.now().plus(TTL)));
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }
}

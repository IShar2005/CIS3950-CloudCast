package com.cloudcast.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherCacheTest {

    private WeatherCache cache;

    @BeforeEach
    void setUp() {
        cache = new WeatherCache();
    }

    @Test
    void get_returnsNullForMissingKey() {
        assertNull(cache.get("nonexistent"));
    }

    @Test
    void put_thenGet_returnsSameValue() {
        cache.put("key1", "hello");
        assertEquals("hello", cache.get("key1"));
    }

    @Test
    void put_thenGet_returnsTypedObject() {
        cache.put("weather:miami", 42);
        Integer value = cache.get("weather:miami");
        assertEquals(42, value);
    }

    @Test
    void clear_removesAllEntries() {
        cache.put("a", 1);
        cache.put("b", 2);
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void put_overwritesExistingValue() {
        cache.put("key", "first");
        cache.put("key", "second");
        assertEquals("second", cache.get("key"));
    }
}

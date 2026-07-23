package com.cloudcast.service;

import com.cloudcast.model.WeatherDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    private WeatherService service;
    private WeatherCache cache;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        cache = new WeatherCache();
        alertService = new AlertService();
        service = new WeatherService(cache, alertService);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://api.openweathermap.org/data/2.5");
    }

    @Test
    void cachePopulated_returnsCachedValueWithoutApiCall() throws Exception {
        WeatherDto cached = new WeatherDto("Miami", 82.0, 88.0, 70,
                8.0, "clear sky", "01d", "imperial", null);
        cache.put("current:city:miami:imperial", cached);
        WeatherDto result = service.getCurrentWeatherByCity("Miami", "imperial");
        assertEquals("Miami", result.city());
        assertEquals(82.0, result.temperature());
    }

    @Test
    void cachePopulatedForCoords_returnsCachedValueWithoutApiCall() throws Exception {
        WeatherDto cached = new WeatherDto("Test City", 60.0, 58.0, 50,
                10.0, "clouds", "03d", "imperial", null);
        cache.put("current:coords:25.700,-80.200:imperial", cached);
        WeatherDto result = service.getCurrentWeatherByCoords(25.7, -80.2, "imperial");
        assertEquals("Test City", result.city());
    }

    @Test
    void differentUnits_useDifferentCacheKeys() {
        WeatherDto imperialCached = new WeatherDto("Miami", 82.0, 88.0, 70,
                8.0, "clear", "01d", "imperial", null);
        cache.put("current:city:miami:imperial", imperialCached);
        assertNull(cache.get("current:city:miami:metric"));
    }
}

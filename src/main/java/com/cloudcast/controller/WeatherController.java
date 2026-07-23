package com.cloudcast.controller;

import com.cloudcast.model.WeatherDto;
import com.cloudcast.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for current weather.
 * GET /api/weather?city=Miami                    (by city name)
 * GET /api/weather?lat=25.7&lon=-80.2            (by coordinates)
 * Both variants accept an optional units=imperial|metric parameter.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherDto getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "imperial") String units
    ) throws Exception {
        if (city != null && !city.isBlank()) {
            return weatherService.getCurrentWeatherByCity(city, units);
        }
        if (lat != null && lon != null) {
            return weatherService.getCurrentWeatherByCoords(lat, lon, units);
        }
        throw new IllegalArgumentException("Provide either 'city' or both 'lat' and 'lon'.");
    }
}

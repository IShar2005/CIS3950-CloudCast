package com.cloudcast.controller;

import com.cloudcast.model.ForecastDto;
import com.cloudcast.service.ForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for 5-day forecast.
 * GET /api/forecast?city=Miami                    (by city name)
 * GET /api/forecast?lat=25.7&lon=-80.2            (by coordinates)
 * Both variants accept an optional units=imperial|metric parameter.
 */
@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    public ForecastDto getForecast(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "imperial") String units
    ) throws Exception {
        if (city != null && !city.isBlank()) {
            return forecastService.getForecastByCity(city, units);
        }
        if (lat != null && lon != null) {
            return forecastService.getForecastByCoords(lat, lon, units);
        }
        throw new IllegalArgumentException("Provide either 'city' or both 'lat' and 'lon'.");
    }
}

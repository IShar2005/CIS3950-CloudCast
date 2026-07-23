package com.cloudcast.model;

/**
 * Current weather data returned to the frontend.
 * Includes an optional alert if severe conditions are detected.
 */
public record WeatherDto(
        String city,
        double temperature,
        double feelsLike,
        int humidity,
        double windSpeed,
        String description,
        String icon,
        String units,
        AlertDto alert
) {}

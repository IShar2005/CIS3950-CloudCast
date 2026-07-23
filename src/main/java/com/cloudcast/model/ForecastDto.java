package com.cloudcast.model;

import java.util.List;

/**
 * Five-day forecast response with a summary per day.
 */
public record ForecastDto(String city, String units, List<ForecastDay> days) {}

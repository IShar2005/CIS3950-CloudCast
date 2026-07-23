package com.cloudcast.model;

/**
 * Single day summary in the 5-day forecast.
 * Date is ISO format (YYYY-MM-DD).
 */
public record ForecastDay(
        String date,
        double tempHigh,
        double tempLow,
        String description,
        String icon
) {}

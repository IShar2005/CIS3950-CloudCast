package com.cloudcast.service;

import com.cloudcast.model.AlertDto;
import org.springframework.stereotype.Service;

/**
 * Evaluates weather conditions and returns an alert if thresholds are exceeded.
 * Thresholds were tuned during Sprint 3 based on real-city testing
 * (Phoenix for heat, Anchorage for cold, Miami for storms).
 */
@Service
public class AlertService {

    // Fahrenheit thresholds
    private static final double EXTREME_HEAT = 100.0;
    private static final double EXTREME_COLD = 20.0;
    // Wind speed in mph
    private static final double HIGH_WIND = 40.0;

    /**
     * Returns an AlertDto if a severity condition is met, otherwise null.
     * severeCondition is the "main" weather condition string from OpenWeatherMap.
     */
    public AlertDto evaluate(double tempFahrenheit, double windSpeedMph, String weatherMain) {
        String conditionLower = weatherMain == null ? "" : weatherMain.toLowerCase();

        // Severe: thunderstorm, tornado
        if (conditionLower.contains("thunderstorm") || conditionLower.contains("tornado")) {
            return new AlertDto("severe", "Severe storm activity in the area. Stay indoors if possible.");
        }

        // Severe: extreme heat or cold
        if (tempFahrenheit >= EXTREME_HEAT) {
            return new AlertDto("severe",
                    "Extreme heat warning. Stay hydrated and avoid prolonged sun exposure.");
        }
        if (tempFahrenheit <= EXTREME_COLD) {
            return new AlertDto("severe",
                    "Extreme cold warning. Limit time outdoors and dress in layers.");
        }

        // Warning: high winds
        if (windSpeedMph >= HIGH_WIND) {
            return new AlertDto("warning",
                    "High wind advisory. Secure loose outdoor items.");
        }

        // Warning: snow or heavy rain
        if (conditionLower.contains("snow")) {
            return new AlertDto("warning", "Snow expected. Drive carefully.");
        }
        if (conditionLower.contains("heavy rain") || conditionLower.contains("squall")) {
            return new AlertDto("warning", "Heavy precipitation expected.");
        }

        return null;
    }
}

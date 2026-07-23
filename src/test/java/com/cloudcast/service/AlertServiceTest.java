package com.cloudcast.service;

import com.cloudcast.model.AlertDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertServiceTest {

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService();
    }

    @Test
    void evaluate_returnsNullForNormalConditions() {
        AlertDto alert = alertService.evaluate(75.0, 8.0, "Clouds");
        assertNull(alert);
    }

    @Test
    void evaluate_returnsSevereForThunderstorm() {
        AlertDto alert = alertService.evaluate(75.0, 15.0, "Thunderstorm");
        assertNotNull(alert);
        assertEquals("severe", alert.level());
        assertTrue(alert.reason().toLowerCase().contains("storm"));
    }

    @Test
    void evaluate_returnsSevereForExtremeHeat() {
        AlertDto alert = alertService.evaluate(102.0, 5.0, "Clear");
        assertNotNull(alert);
        assertEquals("severe", alert.level());
        assertTrue(alert.reason().toLowerCase().contains("heat"));
    }

    @Test
    void evaluate_returnsSevereForExtremeCold() {
        AlertDto alert = alertService.evaluate(15.0, 5.0, "Snow");
        assertNotNull(alert);
        assertEquals("severe", alert.level());
        assertTrue(alert.reason().toLowerCase().contains("cold"));
    }

    @Test
    void evaluate_returnsWarningForHighWind() {
        AlertDto alert = alertService.evaluate(70.0, 45.0, "Clouds");
        assertNotNull(alert);
        assertEquals("warning", alert.level());
        assertTrue(alert.reason().toLowerCase().contains("wind"));
    }

    @Test
    void evaluate_handlesNullConditionSafely() {
        AlertDto alert = alertService.evaluate(75.0, 8.0, null);
        assertNull(alert);
    }
}

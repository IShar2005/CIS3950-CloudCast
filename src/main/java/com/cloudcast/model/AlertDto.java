package com.cloudcast.model;

/**
 * Severe weather alert with a severity level and human-readable reason.
 * level is one of: "warning" or "severe". null field on WeatherDto means no alert.
 */
public record AlertDto(String level, String reason) {}

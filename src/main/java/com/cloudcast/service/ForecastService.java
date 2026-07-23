package com.cloudcast.service;

import com.cloudcast.model.ForecastDay;
import com.cloudcast.model.ForecastDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles 5-day forecast lookups from OpenWeatherMap.
 * The API returns 40 entries (every 3 hours for 5 days). We trim to
 * one summary per day, picking the mid-day forecast for the icon and
 * description, and aggregating high/low temps.
 */
@Service
public class ForecastService {

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final WeatherCache cache;

    public ForecastService(WeatherCache cache) {
        this.cache = cache;
    }

    public ForecastDto getForecastByCity(String city, String units) throws Exception {
        String normalizedUnits = normalizeUnits(units);
        String cacheKey = "forecast:city:" + city.toLowerCase() + ":" + normalizedUnits;
        ForecastDto cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format("%s/forecast?q=%s&appid=%s&units=%s",
                baseUrl, encodedCity, apiKey, normalizedUnits);

        ForecastDto result = fetchAndParse(url, normalizedUnits);
        cache.put(cacheKey, result);
        return result;
    }

    public ForecastDto getForecastByCoords(double lat, double lon, String units) throws Exception {
        String normalizedUnits = normalizeUnits(units);
        String cacheKey = String.format("forecast:coords:%.3f,%.3f:%s", lat, lon, normalizedUnits);
        ForecastDto cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String url = String.format("%s/forecast?lat=%s&lon=%s&appid=%s&units=%s",
                baseUrl, lat, lon, apiKey, normalizedUnits);

        ForecastDto result = fetchAndParse(url, normalizedUnits);
        cache.put(cacheKey, result);
        return result;
    }

    private ForecastDto fetchAndParse(String url, String units) throws Exception {
        String json = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(json);
        String city = root.get("city").get("name").asText();

        // Group entries by date (YYYY-MM-DD), track high/low, and pick midday for icon.
        Map<String, DayAggregator> byDate = new HashMap<>();
        List<String> orderedDates = new ArrayList<>();

        for (JsonNode entry : root.get("list")) {
            String dtTxt = entry.get("dt_txt").asText();
            String date = dtTxt.substring(0, 10);
            String time = dtTxt.substring(11, 16);

            double temp = entry.get("main").get("temp").asDouble();
            String desc = entry.get("weather").get(0).get("description").asText();
            String icon = entry.get("weather").get(0).get("icon").asText();

            DayAggregator agg = byDate.get(date);
            if (agg == null) {
                agg = new DayAggregator(date);
                byDate.put(date, agg);
                orderedDates.add(date);
            }
            agg.observe(temp, desc, icon, time);
        }

        List<ForecastDay> days = new ArrayList<>();
        // Skip the first date if it's "today" and we already have partial data,
        // to avoid a skewed first card. Take the next 5 days available.
        int limit = Math.min(5, orderedDates.size());
        int startIndex = 0;
        // If the first date has fewer than 4 entries, it's likely a partial day
        if (!orderedDates.isEmpty() && byDate.get(orderedDates.get(0)).entryCount < 4 && orderedDates.size() > 5) {
            startIndex = 1;
        }
        for (int i = startIndex; i < orderedDates.size() && days.size() < 5; i++) {
            days.add(byDate.get(orderedDates.get(i)).toForecastDay());
        }

        return new ForecastDto(city, units, days);
    }

    private String normalizeUnits(String units) {
        if (units == null || units.isBlank()) {
            return "imperial";
        }
        String lower = units.toLowerCase();
        if (lower.equals("metric") || lower.equals("imperial")) {
            return lower;
        }
        return "imperial";
    }

    private static class DayAggregator {
        final String date;
        double tempHigh = Double.NEGATIVE_INFINITY;
        double tempLow = Double.POSITIVE_INFINITY;
        String middayDescription = "";
        String middayIcon = "";
        String bestTime = "";
        int entryCount = 0;

        DayAggregator(String date) {
            this.date = date;
        }

        void observe(double temp, String desc, String icon, String time) {
            entryCount++;
            if (temp > tempHigh) tempHigh = temp;
            if (temp < tempLow) tempLow = temp;
            // Pick the entry closest to noon (12:00) for the display icon
            if (isCloserToNoon(time, bestTime)) {
                middayDescription = desc;
                middayIcon = icon;
                bestTime = time;
            }
        }

        private boolean isCloserToNoon(String candidate, String current) {
            if (current.isEmpty()) return true;
            int candMinutes = timeToMinutes(candidate);
            int currMinutes = timeToMinutes(current);
            return Math.abs(candMinutes - 720) < Math.abs(currMinutes - 720);
        }

        private int timeToMinutes(String time) {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        }

        ForecastDay toForecastDay() {
            return new ForecastDay(date, tempHigh, tempLow, middayDescription, middayIcon);
        }
    }
}

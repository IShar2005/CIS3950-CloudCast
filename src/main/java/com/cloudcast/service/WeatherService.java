package com.cloudcast.service;

import com.cloudcast.model.AlertDto;
import com.cloudcast.model.WeatherDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles current weather lookups from OpenWeatherMap.
 * Supports both city name and lat/lon queries.
 * Wraps calls in the WeatherCache to avoid rate limits.
 * Attaches severe weather alerts from AlertService.
 */
@Service
public class WeatherService {

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final WeatherCache cache;
    private final AlertService alertService;

    public WeatherService(WeatherCache cache, AlertService alertService) {
        this.cache = cache;
        this.alertService = alertService;
    }

    public WeatherDto getCurrentWeatherByCity(String city, String units) throws Exception {
        String normalizedUnits = normalizeUnits(units);
        String cacheKey = "current:city:" + city.toLowerCase() + ":" + normalizedUnits;
        WeatherDto cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format("%s/weather?q=%s&appid=%s&units=%s",
                baseUrl, encodedCity, apiKey, normalizedUnits);

        WeatherDto result = fetchAndParse(url, normalizedUnits);
        cache.put(cacheKey, result);
        return result;
    }

    public WeatherDto getCurrentWeatherByCoords(double lat, double lon, String units) throws Exception {
        String normalizedUnits = normalizeUnits(units);
        String cacheKey = String.format("current:coords:%.3f,%.3f:%s", lat, lon, normalizedUnits);
        WeatherDto cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String url = String.format("%s/weather?lat=%s&lon=%s&appid=%s&units=%s",
                baseUrl, lat, lon, apiKey, normalizedUnits);

        WeatherDto result = fetchAndParse(url, normalizedUnits);
        cache.put(cacheKey, result);
        return result;
    }

    private WeatherDto fetchAndParse(String url, String units) throws Exception {
        String json = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(json);

        String city = root.get("name").asText();
        double temp = root.get("main").get("temp").asDouble();
        double feelsLike = root.get("main").get("feels_like").asDouble();
        int humidity = root.get("main").get("humidity").asInt();
        double windSpeed = root.get("wind").get("speed").asDouble();
        String description = root.get("weather").get(0).get("description").asText();
        String icon = root.get("weather").get(0).get("icon").asText();
        String weatherMain = root.get("weather").get(0).get("main").asText();

        // Alerts always evaluated in imperial units (thresholds are Fahrenheit and mph)
        double tempF = "imperial".equals(units) ? temp : (temp * 9 / 5) + 32;
        double windMph = "imperial".equals(units) ? windSpeed : windSpeed * 2.237;
        AlertDto alert = alertService.evaluate(tempF, windMph, weatherMain);

        return new WeatherDto(city, temp, feelsLike, humidity, windSpeed,
                description, icon, units, alert);
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
}

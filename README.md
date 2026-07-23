# CloudCast

A lightweight, accessible weather web application built with Spring Boot and vanilla JavaScript. Search by city or geolocation, view current conditions and 5-day forecasts, save favorite locations, toggle between Fahrenheit and Celsius, and get severe weather alerts. Fully responsive across desktop, tablet, and mobile.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

**Course:** CIS3950 Capstone · FIU Summer 2026
**Live demo:** _add deployed Render URL here_

## Features

- **Current weather** for any city (temperature, feels-like, humidity, wind, description)
- **5-day forecast** with high/low, conditions, and icons
- **Geolocation support** — one-click "Use my location" with browser permission
- **Save favorites** — clickable chips backed by localStorage, no account needed
- **Unit toggle** — Fahrenheit/Celsius with mph/kph, preference persists across sessions
- **Severe weather alerts** — colored banner for extreme heat, cold, thunderstorms, high wind
- **In-memory caching** — 5-minute TTL reduces API calls and avoids rate limits
- **Responsive design** — mobile-first CSS with a 600 px breakpoint
- **Accessible** — alt text on icons, ARIA labels, keyboard navigation, VoiceOver tested
- **Error handling** — friendly messages for invalid cities, rate limits, and API failures

## Screenshots

_Add screenshots after deployment:_

- `docs/screenshots/desktop.png` — desktop view showing search, current weather, and forecast
- `docs/screenshots/mobile.png` — mobile view with stacked forecast cards
- `docs/screenshots/alert.png` — severe weather alert banner in action

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.5.14 · Java 21 |
| Frontend | Vanilla HTML, CSS, JavaScript (no frameworks) |
| Data source | [OpenWeatherMap API](https://openweathermap.org/api) |
| Build | Maven |
| Cache | ConcurrentHashMap with 5-minute TTL |
| Persistence | Browser localStorage (favorites and unit preference) |
| Hosting | [Render](https://render.com) with HTTPS |
| Testing | JUnit 5 · Spring MockMvc |

## Architecture

```
┌─────────────────────┐        ┌──────────────────────┐        ┌──────────────────────┐
│  Browser (HTML/JS)  │  HTTPS │  Spring Boot Backend │  HTTPS │  OpenWeatherMap API  │
│                     │───────▶│                      │───────▶│                      │
│  - Search           │        │  - WeatherController │        │  /weather            │
│  - Geolocation      │        │  - ForecastController│        │  /forecast           │
│  - Favorites        │        │  - WeatherService    │        │                      │
│    (localStorage)   │        │  - ForecastService   │        │                      │
│  - Unit toggle      │        │  - AlertService      │        │                      │
│  - Alert banner     │        │  - WeatherCache      │        │                      │
│                     │◀───────│    (5-min TTL)       │◀───────│                      │
└─────────────────────┘        └──────────────────────┘        └──────────────────────┘
```

**Data flow:**
1. User types a city (or clicks "Use my location") in the browser.
2. Frontend calls `GET /api/weather?city=...` and `GET /api/forecast?city=...`.
3. Spring Boot checks the in-memory cache. On miss, calls OpenWeatherMap.
4. The `AlertService` inspects the response and attaches a severity level if thresholds are exceeded.
5. Response returned as clean JSON; frontend renders the weather card, forecast strip, and any alert.
6. Favorites and unit preference stay in browser localStorage.

## Getting Started

### Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Maven 3.9+ (or use the included `mvnw` wrapper)
- An [OpenWeatherMap API key](https://openweathermap.org/api) (free tier is fine)

### Local Setup

```bash
# Clone the repo
git clone https://github.com/IShar2005/CIS3950-CloudCast.git
cd CIS3950-CloudCast

# Create your local config from the example template
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Edit application.properties and paste in your API key
# (This file is gitignored so your key won't be committed)
```

Your `application.properties` should look like:

```properties
openweather.api.key=your_actual_key_here
openweather.api.base-url=https://api.openweathermap.org/data/2.5
server.port=8080
spring.application.name=cloudcast
```

### Run

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

The app will be available at [http://localhost:8080](http://localhost:8080).

### Test

```bash
./mvnw test
```

You should see 14 unit tests and 2 integration tests all passing.

## API Reference

### `GET /api/weather`

Returns current weather for a city or coordinates.

**Query parameters (one of):**
- `city` — city name (e.g., `Miami`)
- `lat` and `lon` — coordinates (e.g., `lat=25.7&lon=-80.2`)

**Optional:**
- `units` — `imperial` (default) or `metric`

**Example:**
```
GET /api/weather?city=Miami&units=imperial
```

**Response:**
```json
{
  "city": "Miami",
  "temperature": 82.4,
  "feelsLike": 88.1,
  "humidity": 70,
  "windSpeed": 8.05,
  "description": "scattered clouds",
  "icon": "03d",
  "units": "imperial",
  "alert": null
}
```

If severe conditions are detected, `alert` will be an object with `level` (`"warning"` or `"severe"`) and `reason`.

### `GET /api/forecast`

Returns a 5-day forecast with one summary per day.

**Same parameters as `/api/weather`.**

**Response:**
```json
{
  "city": "Miami",
  "units": "imperial",
  "days": [
    { "date": "2026-07-27", "tempHigh": 88.0, "tempLow": 74.0, "description": "sunny", "icon": "01d" },
    ...
  ]
}
```

## Deployment (Render)

CloudCast is deployed on Render's free tier with HTTPS. Steps to deploy your own:

1. **Sign up** at [render.com](https://render.com) and connect your GitHub account.
2. **Create a Web Service** from the CloudCast repository. Render will detect the `Dockerfile` and `render.yaml`.
3. **Set the environment variable** `OPENWEATHER_API_KEY` in the Render dashboard (Environment tab).
4. **Deploy.** Render builds the jar and starts the container.
5. **HTTPS is included by default** at your `.onrender.com` URL, which enables Safari geolocation.

**Known limitation:** the Render free tier spins down after 15 minutes of inactivity. The first request after inactivity takes 10–20 seconds while the container cold-starts. Subsequent requests are fast.

## Project Structure

```
CIS3950-CloudCast/
├── src/
│   ├── main/
│   │   ├── java/com/cloudcast/
│   │   │   ├── CloudcastApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── WeatherController.java
│   │   │   │   ├── ForecastController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/
│   │   │   │   ├── WeatherDto.java
│   │   │   │   ├── ForecastDto.java
│   │   │   │   ├── ForecastDay.java
│   │   │   │   └── AlertDto.java
│   │   │   └── service/
│   │   │       ├── WeatherService.java
│   │   │       ├── ForecastService.java
│   │   │       ├── AlertService.java
│   │   │       └── WeatherCache.java
│   │   └── resources/
│   │       ├── application.properties.example
│   │       └── static/
│   │           ├── index.html
│   │           ├── css/style.css
│   │           └── js/script.js
│   └── test/
│       └── java/com/cloudcast/
│           ├── controller/WeatherControllerIntegrationTest.java
│           └── service/
│               ├── AlertServiceTest.java
│               ├── WeatherCacheTest.java
│               └── WeatherServiceTest.java
├── Dockerfile
├── render.yaml
├── pom.xml
└── README.md
```

## Development Process

CloudCast was built across **5 two-week sprints** using Scrum:

| Sprint | Dates | Focus | Stories |
|--------|-------|-------|---------|
| Sprint 1 | May 18 – Jun 1 | Foundation & MVP | Setup, API integration, search & display |
| Sprint 2 | Jun 2 – Jun 15 | Forecasts & Location | 5-day forecast, geolocation, icons |
| Sprint 3 | Jun 16 – Jun 29 | User Features | Favorites, unit toggle, severe alerts |
| Sprint 4 | Jun 30 – Jul 13 | Polish & Quality | Responsive design, error handling, testing |
| Sprint 5 | Jul 14 – Jul 27 | Deploy & Deliver | Accessibility, deployment, documentation |

**Ceremonies:** 5 sprint plannings · 45+ daily scrums · 5 backlog groomings · 5 sprint reviews · 5 retrospectives.

**Outcome:** 15 of 15 user stories accepted, zero rejected.

## Future Work

Outside the scope of this course but worth exploring:

- **Dark mode** with a light/dark theme toggle
- **PWA support** so CloudCast installs on mobile home screens and caches offline
- **Historical weather** using OpenWeatherMap's Time Machine endpoint
- **Backend database** for favorites so they sync across devices
- **Custom alert thresholds** so users can set their own definitions of severe weather

## Team

| Role | Name |
|------|------|
| Team Leader | Isa Sharief |
| Team Member | Alejandro Perez Domenech |
| Team Member | Nghia Nguyen |
| Instructor | Prof. Masoud Sadjadi |

## Acknowledgments

Thanks to Prof. Masoud Sadjadi and the FIU Knight Foundation School of Computing and Information Sciences for guidance throughout the capstone. Weather data provided by [OpenWeatherMap](https://openweathermap.org).

## License

MIT

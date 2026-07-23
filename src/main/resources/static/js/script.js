// CloudCast frontend logic

const STORAGE_KEYS = {
    UNITS: 'cloudcast.units',
    FAVORITES: 'cloudcast.favorites'
};

const DAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

// DOM refs
const form = document.getElementById('search-form');
const cityInput = document.getElementById('city-input');
const geoBtn = document.getElementById('geo-btn');
const results = document.getElementById('results');
const forecastEl = document.getElementById('forecast');
const alertBanner = document.getElementById('alert-banner');
const favoritesEl = document.getElementById('favorites');
const unitImperialBtn = document.getElementById('unit-imperial');
const unitMetricBtn = document.getElementById('unit-metric');

// State
let currentUnits = loadUnits();
let lastQuery = null; // {type: 'city'|'coords', city?: string, lat?: number, lon?: number}

// --- Initialization ---
renderUnits();
renderFavorites();

// --- Event listeners ---
form.addEventListener('submit', (e) => {
    e.preventDefault();
    const city = cityInput.value.trim();
    if (!city) return;
    lastQuery = { type: 'city', city };
    loadWeather();
});

geoBtn.addEventListener('click', () => {
    if (!navigator.geolocation) {
        showError('Geolocation is not supported by your browser.');
        return;
    }
    showLoading();
    navigator.geolocation.getCurrentPosition(
        (position) => {
            lastQuery = {
                type: 'coords',
                lat: position.coords.latitude,
                lon: position.coords.longitude
            };
            loadWeather();
        },
        (error) => {
            let message = 'Could not get your location.';
            if (error.code === error.PERMISSION_DENIED) {
                message = 'Location permission was denied. You can still search by city name.';
            } else if (error.code === error.POSITION_UNAVAILABLE) {
                message = 'Location information is unavailable.';
            } else if (error.code === error.TIMEOUT) {
                message = 'Location request timed out. Try again.';
            }
            showError(message);
        },
        { timeout: 10000 }
    );
});

unitImperialBtn.addEventListener('click', () => setUnits('imperial'));
unitMetricBtn.addEventListener('click', () => setUnits('metric'));

// --- Core weather loading ---
async function loadWeather() {
    if (!lastQuery) return;
    showLoading();
    hideAlert();
    forecastEl.innerHTML = '';

    try {
        const [weather, forecast] = await Promise.all([
            fetchWeather(),
            fetchForecast()
        ]);
        displayWeather(weather);
        displayForecast(forecast);
        if (weather.alert) {
            displayAlert(weather.alert);
        }
    } catch (err) {
        showError(err.message);
    }
}

async function fetchWeather() {
    const url = buildUrl('/api/weather');
    const response = await fetch(url);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || 'Could not get weather data.');
    }
    return response.json();
}

async function fetchForecast() {
    const url = buildUrl('/api/forecast');
    const response = await fetch(url);
    if (!response.ok) {
        // Forecast failure shouldn't block current weather display
        return null;
    }
    return response.json();
}

function buildUrl(base) {
    const params = new URLSearchParams();
    if (lastQuery.type === 'city') {
        params.set('city', lastQuery.city);
    } else {
        params.set('lat', lastQuery.lat);
        params.set('lon', lastQuery.lon);
    }
    params.set('units', currentUnits);
    return `${base}?${params.toString()}`;
}

// --- Rendering ---
function displayWeather(data) {
    const iconUrl = `https://openweathermap.org/img/wn/${data.icon}@2x.png`;
    const tempUnit = data.units === 'imperial' ? '°F' : '°C';
    const windUnit = data.units === 'imperial' ? 'mph' : 'kph';
    const windValue = data.units === 'imperial'
        ? data.windSpeed
        : (data.windSpeed * 3.6); // API returns m/s in metric; convert to kph

    const isFavorited = getFavorites().includes(data.city);
    const favButtonLabel = isFavorited ? 'Saved to favorites' : 'Add to favorites';
    const favButtonClass = isFavorited ? 'add-favorite saved' : 'add-favorite';

    results.innerHTML = `
        <div class="weather-card">
            <div class="city">${escapeHtml(data.city)}</div>
            <img src="${iconUrl}" alt="${escapeHtml(data.description)}">
            <div class="temp">${Math.round(data.temperature)}${tempUnit}</div>
            <div class="description">${escapeHtml(data.description)}</div>
            <div class="weather-details">
                <div class="detail">
                    <div class="label">Feels Like</div>
                    <div class="value">${Math.round(data.feelsLike)}${tempUnit}</div>
                </div>
                <div class="detail">
                    <div class="label">Humidity</div>
                    <div class="value">${data.humidity}%</div>
                </div>
                <div class="detail">
                    <div class="label">Wind</div>
                    <div class="value">${windValue.toFixed(1)} ${windUnit}</div>
                </div>
            </div>
            <button type="button" class="${favButtonClass}" id="add-fav-btn" ${isFavorited ? 'disabled' : ''}>
                ${isFavorited ? '★ Saved' : '☆ Add to favorites'}
            </button>
        </div>
    `;

    const addFavBtn = document.getElementById('add-fav-btn');
    if (addFavBtn && !isFavorited) {
        addFavBtn.addEventListener('click', () => {
            addFavorite(data.city);
            addFavBtn.textContent = '★ Saved';
            addFavBtn.classList.add('saved');
            addFavBtn.disabled = true;
        });
    }
}

function displayForecast(data) {
    if (!data || !data.days || data.days.length === 0) {
        forecastEl.innerHTML = '';
        return;
    }
    const tempUnit = data.units === 'imperial' ? '°F' : '°C';
    forecastEl.innerHTML = data.days.map(day => {
        const dateObj = new Date(day.date + 'T00:00:00');
        const dayLabel = DAY_LABELS[dateObj.getDay()];
        const iconUrl = `https://openweathermap.org/img/wn/${day.icon}.png`;
        return `
            <div class="forecast-card">
                <div class="day">${dayLabel}</div>
                <img src="${iconUrl}" alt="${escapeHtml(day.description)}">
                <span class="high">${Math.round(day.tempHigh)}${tempUnit}</span>
                <span class="low">${Math.round(day.tempLow)}${tempUnit}</span>
            </div>
        `;
    }).join('');
}

function displayAlert(alert) {
    alertBanner.className = `alert-banner ${alert.level}`;
    const title = alert.level === 'severe' ? 'Severe Weather Alert' : 'Weather Advisory';
    alertBanner.innerHTML = `
        <div class="alert-title">${title}</div>
        <div>${escapeHtml(alert.reason)}</div>
    `;
    alertBanner.hidden = false;
}

function hideAlert() {
    alertBanner.hidden = true;
    alertBanner.innerHTML = '';
}

function showLoading() {
    results.innerHTML = '<div class="loading"><span class="spinner" aria-hidden="true"></span> Loading...</div>';
    hideAlert();
    forecastEl.innerHTML = '';
}

function showError(message) {
    results.innerHTML = `<div class="error" role="alert">${escapeHtml(message)}</div>`;
    hideAlert();
    forecastEl.innerHTML = '';
}

// --- Units ---
function setUnits(units) {
    if (currentUnits === units) return;
    currentUnits = units;
    localStorage.setItem(STORAGE_KEYS.UNITS, units);
    renderUnits();
    if (lastQuery) {
        loadWeather();
    }
}

function renderUnits() {
    unitImperialBtn.classList.toggle('active', currentUnits === 'imperial');
    unitMetricBtn.classList.toggle('active', currentUnits === 'metric');
    unitImperialBtn.setAttribute('aria-pressed', currentUnits === 'imperial');
    unitMetricBtn.setAttribute('aria-pressed', currentUnits === 'metric');
}

function loadUnits() {
    return localStorage.getItem(STORAGE_KEYS.UNITS) || 'imperial';
}

// --- Favorites ---
function getFavorites() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.FAVORITES);
        return raw ? JSON.parse(raw) : [];
    } catch (e) {
        return [];
    }
}

function saveFavorites(list) {
    localStorage.setItem(STORAGE_KEYS.FAVORITES, JSON.stringify(list));
}

function addFavorite(city) {
    const list = getFavorites();
    if (!list.includes(city)) {
        list.push(city);
        saveFavorites(list);
        renderFavorites();
    }
}

function removeFavorite(city) {
    const list = getFavorites().filter(c => c !== city);
    saveFavorites(list);
    renderFavorites();
}

function renderFavorites() {
    const list = getFavorites();
    favoritesEl.innerHTML = list.map(city => `
        <div class="chip" tabindex="0" role="button" data-city="${escapeAttr(city)}" aria-label="Load weather for ${escapeAttr(city)}">
            <span>${escapeHtml(city)}</span>
            <button type="button" class="chip-remove" data-remove="${escapeAttr(city)}" aria-label="Remove ${escapeAttr(city)} from favorites">×</button>
        </div>
    `).join('');

    favoritesEl.querySelectorAll('.chip').forEach(chip => {
        const city = chip.dataset.city;
        chip.addEventListener('click', (e) => {
            if (e.target.closest('.chip-remove')) return;
            cityInput.value = city;
            lastQuery = { type: 'city', city };
            loadWeather();
        });
        chip.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                if (e.target.closest('.chip-remove')) return;
                e.preventDefault();
                cityInput.value = city;
                lastQuery = { type: 'city', city };
                loadWeather();
            }
        });
    });

    favoritesEl.querySelectorAll('.chip-remove').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            removeFavorite(btn.dataset.remove);
        });
    });
}

// --- Utilities ---
function escapeHtml(text) {
    if (text == null) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeAttr(text) {
    return escapeHtml(text);
}

package com.sinag;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fetches live weather from Google Weather API (with Open-Meteo fallback if no key is set). */
public class WeatherService {
    // Paste your Google Maps Platform Weather API key here:
    private static final String GOOGLE_API_KEY = "YOUR_GOOGLE_WEATHER_API_KEY";

    // Default coordinates: Batangas City, Philippines
    private static final double LAT = 13.7565;
    private static final double LON = 121.0583;
    private static final String LOCATION_NAME = "Batangas City";

    public static String fetchLiveWeather() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .build();

            // Use Google Weather API if a valid key is configured
            if (GOOGLE_API_KEY != null && !GOOGLE_API_KEY.isBlank() && !GOOGLE_API_KEY.equals("YOUR_GOOGLE_WEATHER_API_KEY")) {
                String googleUrl = String.format(
                        "https://weather.googleapis.com/v1/currentConditions:lookup?key=%s&location.latitude=%f&location.longitude=%f",
                        GOOGLE_API_KEY, LAT, LON);

                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(googleUrl)).GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    String json = res.body();
                    String temp = extractNumber(json, "\"temperature\"\\s*:\\s*\\{[^}]*\"degrees\"\\s*:\\s*([0-9.\\-]+)");
                    String humidity = extractNumber(json, "\"relativeHumidity\"\\s*:\\s*([0-9]+)");
                    String desc = extractText(json, "\"description\"\\s*:\\s*\\{[^}]*\"text\"\\s*:\\s*\"([^\"]+)\"");
                    return String.format("%s: %s°C  •  %s  •  Humidity: %s%% (Google Weather)",
                            LOCATION_NAME, temp, desc, humidity);
                }
            }

            // Fallback to Open-Meteo (free, no API key required) so the UI always displays live data
            String fallbackUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,apparent_temperature",
                    LAT, LON);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(fallbackUrl)).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                String json = res.body();
                String temp = extractNumber(json, "\"temperature_2m\"\\s*:\\s*([0-9.\\-]+)");
                String feels = extractNumber(json, "\"apparent_temperature\"\\s*:\\s*([0-9.\\-]+)");
                String humidity = extractNumber(json, "\"relative_humidity_2m\"\\s*:\\s*([0-9]+)");
                return String.format("%s: %s°C (Feels like %s°C)  •  Humidity: %s%%",
                        LOCATION_NAME, temp, feels, humidity);
            }

            return LOCATION_NAME + ": Weather service unavailable (HTTP " + res.statusCode() + ")";
        } catch (Exception e) {
            return LOCATION_NAME + ": Offline — unable to load live weather";
        }
    }

    private static String extractNumber(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        return m.find() ? m.group(1) : "--";
    }

    private static String extractText(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        return m.find() ? m.group(1) : "Clear";
    }
}
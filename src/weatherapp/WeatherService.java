package weatherapp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WeatherService {

    private static final String CURRENT_WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private static final String GEOCODING_URL = "https://api.openweathermap.org/geo/1.0/direct";

    private final String apiKey;

    public WeatherService() {
        this.apiKey = ConfigLoader.getOpenWeatherApiKey();
    }

    public JSONObject fetchCurrentWeather(String location) throws Exception {
        Coordinates coordinates = getCoordinatesForLocation(location);

        String url = CURRENT_WEATHER_URL
                + "?lat=" + coordinates.latitude
                + "&lon=" + coordinates.longitude
                + "&appid=" + apiKey
                + "&units=metric";

        return makeApiCallObject(url);
    }

    public List<DayForecast> fetchForecast(String location) throws Exception {
        Coordinates coordinates = getCoordinatesForLocation(location);

        String url = FORECAST_URL
                + "?lat=" + coordinates.latitude
                + "&lon=" + coordinates.longitude
                + "&appid=" + apiKey
                + "&units=metric";

        JSONObject forecastJson = makeApiCallObject(url);
        JSONArray forecastList = forecastJson.getJSONArray("list");

        Map<String, DayForecast> dailyForecastMap = new LinkedHashMap<>();

        for (int i = 0; i < forecastList.length(); i++) {
            JSONObject entry = forecastList.getJSONObject(i);

            String dateTime = entry.getString("dt_txt");
            String date = dateTime.substring(0, 10);

            JSONObject mainObject = entry.getJSONObject("main");

            double tempMinCelsius = mainObject.getDouble("temp_min");
            double tempMaxCelsius = mainObject.getDouble("temp_max");

            JSONObject weatherObject = entry
                    .getJSONArray("weather")
                    .getJSONObject(0);

            String iconCode = weatherObject.getString("icon");
            String condition = weatherObject.getString("description");

            dailyForecastMap.putIfAbsent(date, new DayForecast(date));

            DayForecast dayForecast = dailyForecastMap.get(date);
            dayForecast.update(tempMinCelsius, iconCode, condition);
            dayForecast.update(tempMaxCelsius, iconCode, condition);
        }

        List<DayForecast> result = new ArrayList<>();

        for (DayForecast forecast : dailyForecastMap.values()) {
            result.add(forecast);

            if (result.size() == 5) {
                break;
            }
        }

        return result;
    }

    private Coordinates getCoordinatesForLocation(String location) throws Exception {
        if (location == null || location.trim().isEmpty()) {
            throw new Exception("Location cannot be empty.");
        }

        String cleanedLocation = cleanLocationInput(location);
        String titleCaseLocation = toTitleCaseLocation(cleanedLocation);

        List<String> attempts = new ArrayList<>();

        attempts.add(cleanedLocation);

        if (!titleCaseLocation.equals(cleanedLocation)) {
            attempts.add(titleCaseLocation);
        }

        String lowerLocation = cleanedLocation.toLowerCase();

        if (!lowerLocation.contains(", us")
                && !lowerLocation.contains(",usa")
                && !lowerLocation.contains(", united states")) {
            attempts.add(titleCaseLocation + ", US");
        }

        Exception lastException = null;

        for (String attempt : attempts) {
            try {
                JSONArray results = geocodeLocation(attempt);

                if (results.length() > 0) {
                    JSONObject firstResult = results.getJSONObject(0);

                    double latitude = firstResult.getDouble("lat");
                    double longitude = firstResult.getDouble("lon");

                    return new Coordinates(latitude, longitude);
                }
            } catch (Exception ex) {
                lastException = ex;
            }
        }

        if (lastException != null) {
            throw new Exception("No matching location found for: " + location + "\n" + lastException.getMessage());
        }

        throw new Exception("No matching location found for: " + location);
    }

    private JSONArray geocodeLocation(String location) throws Exception {
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

        String url = GEOCODING_URL
                + "?q=" + encodedLocation
                + "&limit=5"
                + "&appid=" + apiKey;

        return makeApiCallArray(url);
    }

    private String cleanLocationInput(String location) {
        return location.trim().replaceAll("\\s+", " ");
    }

    private String toTitleCaseLocation(String location) {
        String[] parts = location.split(",");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();

            if (part.equalsIgnoreCase("us")
                    || part.equalsIgnoreCase("usa")
                    || part.equalsIgnoreCase("uk")) {
                result.append(part.toUpperCase());
            } else {
                result.append(toTitleCaseWords(part));
            }

            if (i < parts.length - 1) {
                result.append(", ");
            }
        }

        return result.toString();
    }

    private String toTitleCaseWords(String text) {
        String[] words = text.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));

                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }

                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    private JSONObject makeApiCallObject(String urlString) throws Exception {
        URI uri = URI.create(urlString);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);

        int responseCode = connection.getResponseCode();

        InputStream responseStream;

        if (responseCode >= 200 && responseCode < 300) {
            responseStream = connection.getInputStream();
        } else {
            responseStream = connection.getErrorStream();
            String errorMessage = readResponse(responseStream);
            connection.disconnect();
            throw new Exception("OpenWeatherMap returned HTTP " + responseCode + ": " + errorMessage);
        }

        String responseBody = readResponse(responseStream);
        connection.disconnect();

        return new JSONObject(responseBody);
    }

    private JSONArray makeApiCallArray(String urlString) throws Exception {
        URI uri = URI.create(urlString);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);

        int responseCode = connection.getResponseCode();

        InputStream responseStream;

        if (responseCode >= 200 && responseCode < 300) {
            responseStream = connection.getInputStream();
        } else {
            responseStream = connection.getErrorStream();
            String errorMessage = readResponse(responseStream);
            connection.disconnect();
            throw new Exception("OpenWeatherMap Geocoding returned HTTP " + responseCode + ": " + errorMessage);
        }

        String responseBody = readResponse(responseStream);
        connection.disconnect();

        return new JSONArray(responseBody);
    }

    private String readResponse(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    private static class Coordinates {
        private final double latitude;
        private final double longitude;

        private Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
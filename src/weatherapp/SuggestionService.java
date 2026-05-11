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
import java.util.Collections;
import java.util.List;

public class SuggestionService {

    private static final String GEOCODING_URL = "https://api.openweathermap.org/geo/1.0/direct";

    private final String apiKey;

    public SuggestionService() {
        this.apiKey = ConfigLoader.getOpenWeatherApiKey();
    }

    public List<String> fetchSuggestions(String query) throws Exception {
        if (query == null || query.trim().length() < 3) {
            return Collections.emptyList();
        }

        String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);

        String url = GEOCODING_URL
                + "?q=" + encodedQuery
                + "&limit=5"
                + "&appid=" + apiKey;

        JSONArray resultsArray = makeApiCallArray(url);
        List<String> suggestions = new ArrayList<>();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject location = resultsArray.getJSONObject(i);

            String name = location.optString("name", "");
            String state = location.optString("state", "");
            String country = location.optString("country", "");

            if (!name.isBlank()) {
                StringBuilder suggestion = new StringBuilder(name);

                if (!state.isBlank()) {
                    suggestion.append(", ").append(state);
                }

                if (!country.isBlank()) {
                    suggestion.append(", ").append(country);
                }

                suggestions.add(suggestion.toString());
            }
        }

        return suggestions;
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
            throw new Exception("Geocoding API returned HTTP " + responseCode + ": " + errorMessage);
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
}
package weatherapp;

import java.io.FileInputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String API_KEY_PROPERTY = "OPENWEATHER_API_KEY";

    private ConfigLoader() {
        // Utility class. Prevents creating ConfigLoader objects.
    }

    public static String getOpenWeatherApiKey() {
        Properties properties = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(CONFIG_FILE_NAME)) {
            properties.load(fileInputStream);

            String apiKey = properties.getProperty(API_KEY_PROPERTY);

            if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_api_key_here")) {
                throw new IllegalStateException(API_KEY_PROPERTY + " is missing or invalid in " + CONFIG_FILE_NAME + ".");
            }

            return apiKey.trim();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not load OpenWeatherMap API key. Make sure "
                            + CONFIG_FILE_NAME
                            + " exists in the project root and contains "
                            + API_KEY_PROPERTY
                            + "=your_key_here.",
                    ex
            );
        }
    }
}
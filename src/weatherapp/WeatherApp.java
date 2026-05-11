package weatherapp;

import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class WeatherApp extends JFrame {

    private final WeatherService weatherService;
    private final SuggestionService suggestionService;

    private final JTextField locationField;
    private final JButton searchButton;
    private final JComboBox<String> unitComboBox;

    private final JLabel cityLabel;
    private final JLabel weatherIconLabel;
    private final JLabel temperatureLabel;
    private final JLabel humidityLabel;
    private final JLabel windSpeedLabel;
    private final JLabel conditionLabel;

    private final JPanel forecastPanel;
    private final JTextArea historyArea;

    private final JPopupMenu suggestionsPopup;
    private final DefaultListModel<String> suggestionsListModel;
    private final JList<String> suggestionsList;

    private Timer autoCompleteTimer;

    private final List<String> searchHistory;
    private List<DayForecast> lastFiveDayForecast;

    private Double lastTempCelsius;
    private Double lastWindSpeedMS;

    private final Map<String, SuggestionCache> prefixCache;
    private static final long CACHE_EXPIRATION_MS = 60_000;
    private static final long MIN_GEO_API_INTERVAL_MS = 2_000;
    private long lastGeoApiCallTime = 0;

    public WeatherApp() {
        super("Java Weather App");

        weatherService = new WeatherService();
        suggestionService = new SuggestionService();

        searchHistory = new ArrayList<>();
        lastFiveDayForecast = new ArrayList<>();
        prefixCache = new HashMap<>();

        BackgroundPanel mainPanel = new BackgroundPanel();
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        locationField = new JTextField(20);
        searchButton = new JButton("Search");
        unitComboBox = new JComboBox<>(new String[] { "Fahrenheit", "Celsius" });

        cityLabel = new JLabel("Search for a city", SwingConstants.CENTER);
        weatherIconLabel = new JLabel("☀", SwingConstants.CENTER);
        temperatureLabel = new JLabel("Temperature: N/A", SwingConstants.CENTER);
        humidityLabel = new JLabel("Humidity: N/A", SwingConstants.CENTER);
        windSpeedLabel = new JLabel("Wind Speed: N/A", SwingConstants.CENTER);
        conditionLabel = new JLabel("Condition: N/A", SwingConstants.CENTER);

        forecastPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        forecastPanel.setOpaque(false);

        historyArea = new JTextArea(5, 30);
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);

        suggestionsPopup = new JPopupMenu();
        suggestionsListModel = new DefaultListModel<>();
        suggestionsList = new JList<>(suggestionsListModel);

        buildTopPanel(mainPanel);
        buildCurrentWeatherPanel(mainPanel);
        buildBottomPanel(mainPanel);
        buildSuggestionsPopup();

        attachEventHandlers();

        setSize(950, 760);
        setMinimumSize(new Dimension(900, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void buildTopPanel(JPanel mainPanel) {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        topPanel.setOpaque(false);

        JLabel locationLabel = new JLabel("Enter location:");
        locationLabel.setForeground(Color.WHITE);
        locationLabel.setFont(new Font("Arial", Font.BOLD, 15));

        locationField.setFont(new Font("Arial", Font.PLAIN, 15));
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        unitComboBox.setFont(new Font("Arial", Font.PLAIN, 14));

        topPanel.add(locationLabel);
        topPanel.add(locationField);
        topPanel.add(searchButton);
        topPanel.add(unitComboBox);

        mainPanel.add(topPanel, BorderLayout.NORTH);
    }

    private void buildCurrentWeatherPanel(JPanel mainPanel) {
        JPanel weatherCard = new JPanel();
        weatherCard.setLayout(new BoxLayout(weatherCard, BoxLayout.Y_AXIS));
        weatherCard.setOpaque(true);
        weatherCard.setBackground(new Color(245, 248, 252));
        weatherCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        weatherCard.setPreferredSize(new Dimension(850, 300));

        cityLabel.setFont(new Font("Arial", Font.BOLD, 28));
        weatherIconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        temperatureLabel.setFont(new Font("Arial", Font.BOLD, 24));
        humidityLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        windSpeedLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        conditionLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        weatherIconLabel.setPreferredSize(new Dimension(160, 80));
        weatherIconLabel.setMaximumSize(new Dimension(160, 80));
        weatherIconLabel.setMinimumSize(new Dimension(160, 80));

        cityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        weatherIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        temperatureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        humidityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        windSpeedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        conditionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        weatherCard.add(cityLabel);
        weatherCard.add(Box.createVerticalStrut(6));
        weatherCard.add(weatherIconLabel);
        weatherCard.add(Box.createVerticalStrut(6));
        weatherCard.add(temperatureLabel);
        weatherCard.add(Box.createVerticalStrut(6));
        weatherCard.add(humidityLabel);
        weatherCard.add(Box.createVerticalStrut(6));
        weatherCard.add(windSpeedLabel);
        weatherCard.add(Box.createVerticalStrut(6));
        weatherCard.add(conditionLabel);

        mainPanel.add(weatherCard, BorderLayout.CENTER);
    }

    private void buildBottomPanel(JPanel mainPanel) {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        JLabel forecastTitle = new JLabel("5-Day Forecast", SwingConstants.CENTER);
        forecastTitle.setForeground(Color.WHITE);
        forecastTitle.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel forecastWrapper = new JPanel(new BorderLayout(5, 5));
        forecastWrapper.setOpaque(false);
        forecastWrapper.add(forecastTitle, BorderLayout.NORTH);
        forecastWrapper.add(forecastPanel, BorderLayout.CENTER);

        JLabel historyTitle = new JLabel("Search History");
        historyTitle.setForeground(Color.WHITE);
        historyTitle.setFont(new Font("Arial", Font.BOLD, 16));

        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setPreferredSize(new Dimension(300, 100));

        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setOpaque(false);
        historyPanel.add(historyTitle, BorderLayout.NORTH);
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        bottomPanel.add(forecastWrapper, BorderLayout.CENTER);
        bottomPanel.add(historyPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void buildSuggestionsPopup() {
        suggestionsList.setFocusable(false);
        suggestionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionsList.setFont(new Font("Arial", Font.PLAIN, 14));

        suggestionsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selectedValue = suggestionsList.getSelectedValue();
                if (selectedValue != null) {
                    locationField.setText(selectedValue);
                    suggestionsPopup.setVisible(false);
                    locationField.requestFocusInWindow();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(
                suggestionsList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane.setPreferredSize(new Dimension(260, 120));
        suggestionsPopup.add(scrollPane);
    }

    private void attachEventHandlers() {
        searchButton.addActionListener(e -> searchWeather());

        locationField.addActionListener(e -> searchWeather());

        unitComboBox.addActionListener(e -> {
            if (lastTempCelsius != null && lastWindSpeedMS != null) {
                updateLabelsForSelectedUnit();
                rebuildForecastPanel();
            }
        });

        locationField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleAutoComplete();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleAutoComplete();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleAutoComplete();
            }
        });
    }

    private void searchWeather() {
        String location = locationField.getText().trim();

        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a city or location.",
                    "Missing Location",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private JSONObject currentWeather;
            private List<DayForecast> forecast;

            @Override
            protected Void doInBackground() throws Exception {
                currentWeather = weatherService.fetchCurrentWeather(location);
                forecast = weatherService.fetchForecast(location);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    double tempC = currentWeather.getJSONObject("main").getDouble("temp");
                    double humidity = currentWeather.getJSONObject("main").getDouble("humidity");
                    double windSpeed = currentWeather.getJSONObject("wind").getDouble("speed");

                    JSONObject weatherObject = currentWeather
                            .getJSONArray("weather")
                            .getJSONObject(0);

                    String condition = weatherObject.getString("main");
                    String description = weatherObject.getString("description");
                    String iconCode = weatherObject.getString("icon");

                    String resolvedCityName = currentWeather.optString("name", location);

                    lastFiveDayForecast = forecast;

                    updateCurrentWeatherDisplay(
                            resolvedCityName,
                            tempC,
                            humidity,
                            windSpeed,
                            condition,
                            description,
                            iconCode
                    );

                    addSearchHistory(resolvedCityName, condition);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            WeatherApp.this,
                            "Could not retrieve weather data.\n\n" + ex.getMessage(),
                            "Weather Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    searchButton.setEnabled(true);
                    searchButton.setText("Search");
                }
            }
        };

        worker.execute();
    }

    private void updateCurrentWeatherDisplay(
            String cityName,
            double tempC,
            double humidity,
            double windSpeedMS,
            String condition,
            String description,
            String iconCode
    ) {
        lastTempCelsius = tempC;
        lastWindSpeedMS = windSpeedMS;

        cityLabel.setText(cityName);
        weatherIconLabel.setText(getWeatherEmoji(iconCode, condition, description));
        humidityLabel.setText(String.format("Humidity: %.0f%%", humidity));
        conditionLabel.setText("Condition: " + capitalizeWords(description));

        updateLabelsForSelectedUnit();
        rebuildForecastPanel();
    }

    private void updateLabelsForSelectedUnit() {
        String selectedUnit = (String) unitComboBox.getSelectedItem();

        if ("Celsius".equals(selectedUnit)) {
            temperatureLabel.setText(String.format("Temperature: %.1f °C", lastTempCelsius));
            windSpeedLabel.setText(String.format("Wind Speed: %.1f m/s", lastWindSpeedMS));
        } else {
            double tempF = (lastTempCelsius * 9.0 / 5.0) + 32.0;
            double windMPH = lastWindSpeedMS * 2.23694;

            temperatureLabel.setText(String.format("Temperature: %.1f °F", tempF));
            windSpeedLabel.setText(String.format("Wind Speed: %.1f mph", windMPH));
        }
    }

    private void rebuildForecastPanel() {
        forecastPanel.removeAll();

        if (lastFiveDayForecast == null || lastFiveDayForecast.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                forecastPanel.add(createEmptyForecastCard());
            }
        } else {
            for (DayForecast forecast : lastFiveDayForecast) {
                forecastPanel.add(createForecastCard(forecast));
            }
        }

        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private JPanel createForecastCard(DayForecast forecast) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(245, 248, 252));
        card.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

        JLabel dateLabel = new JLabel(forecast.getDisplayDate(), SwingConstants.CENTER);
        JLabel iconLabel = new JLabel(getWeatherEmoji(
                forecast.getIconCode(),
                forecast.getCondition(),
                forecast.getCondition()
        ), SwingConstants.CENTER);
        JLabel highLabel = new JLabel("High: " + formatTemperature(forecast.getMaxTempCelsius()), SwingConstants.CENTER);
        JLabel lowLabel = new JLabel("Low: " + formatTemperature(forecast.getMinTempCelsius()), SwingConstants.CENTER);
        JLabel conditionLabel = new JLabel(capitalizeWords(forecast.getCondition()), SwingConstants.CENTER);

        dateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        highLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        lowLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        conditionLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        iconLabel.setPreferredSize(new Dimension(80, 42));
        iconLabel.setMaximumSize(new Dimension(80, 42));
        iconLabel.setMinimumSize(new Dimension(80, 42));

        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        highLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        lowLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        conditionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(dateLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(highLabel);
        card.add(lowLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(conditionLabel);

        return card;
    }

    private JPanel createEmptyForecastCard() {
        JPanel card = new JPanel();
        card.setBackground(new Color(245, 248, 252));
        card.add(new JLabel("No forecast"));
        return card;
    }

    private String formatTemperature(double tempC) {
        String selectedUnit = (String) unitComboBox.getSelectedItem();

        if ("Celsius".equals(selectedUnit)) {
            return String.format("%.1f °C", tempC);
        }

        double tempF = (tempC * 9.0 / 5.0) + 32.0;
        return String.format("%.1f °F", tempF);
    }

    private void addSearchHistory(String cityName, String condition) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
        String entry = cityName + " - " + condition + " - " + timestamp;

        searchHistory.add(0, entry);

        if (searchHistory.size() > 8) {
            searchHistory.remove(searchHistory.size() - 1);
        }

        StringBuilder historyText = new StringBuilder();
        for (String item : searchHistory) {
            historyText.append(item).append(System.lineSeparator());
        }

        historyArea.setText(historyText.toString());
    }

    private void scheduleAutoComplete() {
        if (autoCompleteTimer != null) {
            autoCompleteTimer.stop();
        }

        autoCompleteTimer = new Timer(450, e -> showSuggestions());
        autoCompleteTimer.setRepeats(false);
        autoCompleteTimer.start();
    }

    private void showSuggestions() {
        String query = locationField.getText().trim();

        if (query.length() < 3) {
            suggestionsPopup.setVisible(false);
            return;
        }

        List<String> cachedSuggestions = getCachedSuggestions(query);
        if (cachedSuggestions != null) {
            displaySuggestions(cachedSuggestions);
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastGeoApiCallTime < MIN_GEO_API_INTERVAL_MS) {
            suggestionsPopup.setVisible(false);
            return;
        }

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return suggestionService.fetchSuggestions(query);
            }

            @Override
            protected void done() {
                try {
                    List<String> suggestions = get();
                    prefixCache.put(query, new SuggestionCache(suggestions, System.currentTimeMillis()));
                    lastGeoApiCallTime = System.currentTimeMillis();
                    displaySuggestions(suggestions);
                } catch (Exception ex) {
                    suggestionsPopup.setVisible(false);
                }
            }
        };

        worker.execute();
    }

    private List<String> getCachedSuggestions(String query) {
        SuggestionCache cached = prefixCache.get(query);

        if (cached == null) {
            return null;
        }

        long now = System.currentTimeMillis();

        if (now - cached.timestamp > CACHE_EXPIRATION_MS) {
            prefixCache.remove(query);
            return null;
        }

        return cached.suggestions;
    }

    private void displaySuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            suggestionsPopup.setVisible(false);
            return;
        }

        suggestionsListModel.clear();

        for (String suggestion : suggestions) {
            suggestionsListModel.addElement(suggestion);
        }

        suggestionsList.setVisibleRowCount(Math.min(suggestions.size(), 6));
        suggestionsPopup.show(locationField, 0, locationField.getHeight());
        locationField.requestFocusInWindow();
    }

    private String getWeatherEmoji(String iconCode, String condition, String description) {
        String code = iconCode == null ? "" : iconCode;
        String combinedText = ((condition == null ? "" : condition) + " " + (description == null ? "" : description)).toLowerCase();

        if (code.startsWith("01") || combinedText.contains("clear")) {
            return "☀";
        }

        if (code.startsWith("02") || combinedText.contains("few clouds")) {
            return "🌤";
        }

        if (code.startsWith("03") || code.startsWith("04") || combinedText.contains("cloud")) {
            return "☁";
        }

        if (code.startsWith("09") || code.startsWith("10") || combinedText.contains("rain") || combinedText.contains("drizzle")) {
            return "🌧";
        }

        if (code.startsWith("11") || combinedText.contains("thunder")) {
            return "⛈";
        }

        if (code.startsWith("13") || combinedText.contains("snow")) {
            return "❄";
        }

        if (code.startsWith("50") || combinedText.contains("mist") || combinedText.contains("fog") || combinedText.contains("haze")) {
            return "🌫";
        }

        return "🌡";
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isBlank()) {
            return "N/A";
        }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WeatherApp app = new WeatherApp();
            app.setVisible(true);
        });
    }

    private static class SuggestionCache {
        private final List<String> suggestions;
        private final long timestamp;

        private SuggestionCache(List<String> suggestions, long timestamp) {
            this.suggestions = suggestions;
            this.timestamp = timestamp;
        }
    }
}
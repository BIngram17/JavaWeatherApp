package weatherapp;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DayForecast {

    private final String date;
    private double minTempCelsius;
    private double maxTempCelsius;
    private String iconCode;
    private String condition;

    public DayForecast(String date) {
        this.date = date;
        this.minTempCelsius = Double.MAX_VALUE;
        this.maxTempCelsius = -Double.MAX_VALUE;
        this.iconCode = "";
        this.condition = "";
    }

    public void update(double tempCelsius, String iconCode, String condition) {
        if (tempCelsius < minTempCelsius) {
            minTempCelsius = tempCelsius;
        }

        if (tempCelsius > maxTempCelsius) {
            maxTempCelsius = tempCelsius;
        }

        if (iconCode != null && !iconCode.isBlank()) {
            this.iconCode = iconCode;
        }

        if (condition != null && !condition.isBlank()) {
            this.condition = condition;
        }
    }

    public String getDate() {
        return date;
    }

    public String getDisplayDate() {
        LocalDate localDate = LocalDate.parse(date);
        return localDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"));
    }

    public double getMinTempCelsius() {
        return minTempCelsius;
    }

    public double getMaxTempCelsius() {
        return maxTempCelsius;
    }

    public String getIconCode() {
        return iconCode;
    }

    public String getCondition() {
        return condition;
    }
}
//import java.math.BigInteger;
//import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class HUDStringBuilder {
    private final Map<String, Number> data;
    private String cachedFormattedString;
    private final NumberFormat numberFormat;
    private final String delimiter;
    private int lastUpdateFrame;

    public HUDStringBuilder() {
        data = new LinkedHashMap<>(); // Use LinkedHashMap to maintain the insertion order
        cachedFormattedString = "";
        lastUpdateFrame = 0;
        numberFormat = NumberFormat.getInstance();
        delimiter = " | ";
    }

    public void addOrUpdate(String key, Number value) {
        data.put(key, value);
    }

    public String formatLargeNumber(Number value) {
        String[] largeNumberNames = {
                "thousand", "million", "billion", "trillion", "quadrillion", "quintillion", "sextillion",
                "septillion", "octillion", "nonillion", "decillion"
        };

        double doubleValue = value.doubleValue();
        int exponent = (int) Math.floor(Math.log10(doubleValue));
        int index = (exponent - 3) / 3;

        if (index < 0) {
            return numberFormat.format(value);
        } else if (index < largeNumberNames.length) {
            double shortNumber = doubleValue / Math.pow(10, index * 3 + 3);
            return String.format("%.1f %s", shortNumber, largeNumberNames[index]);
        } else {
            return String.format("%.1e", doubleValue);
        }
    }

    public String getFormattedString(int frameCount, int updateFrequency, String delimiter) {
        if (frameCount - lastUpdateFrame >= updateFrequency || cachedFormattedString.isEmpty()) {
            StringBuilder formattedString = new StringBuilder();
            for (Map.Entry<String, Number> entry : data.entrySet()) {
                Number value = entry.getValue();
                String formattedValue;

                if (value.doubleValue() >= Math.pow(10, 9)) {
                    formattedValue = formatLargeNumber(value);
                } else {
                    formattedValue = numberFormat.format(value);
                }

                formattedString.append(entry.getKey())
                        .append(" ")
                        .append(formattedValue)
                        .append(delimiter);
            }
            // Remove the last delimiter
            if (formattedString.length() > 0) {
                formattedString.setLength(formattedString.length() - delimiter.length());
            }
            cachedFormattedString = formattedString.toString();
            lastUpdateFrame = frameCount;
        }
        return cachedFormattedString;
    }

    public String getFormattedString(int frameCount, int updateFrequency) {
        return getFormattedString(frameCount, updateFrequency, this.delimiter);
    }
}

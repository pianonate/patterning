import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class HUDStringBuilder {
    private final Map<String, Object> data; // Changed from Number to Object
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

    public void addOrUpdate(String key, Object value) {
        if (value instanceof Number) {
            data.put(key, value);
        } else {
            throw new IllegalArgumentException("Value must be a Number or BigInteger.");
        }
    }

    public String formatLargeNumber(Object value) {
        if (value instanceof BigInteger bigValue) {
            int exponent = bigValue.toString().length() - 1;
            return formatLargeNumberUsingExponent(new BigDecimal(bigValue), exponent);
        } else {
            Number numValue = (Number) value;
            double doubleValue = numValue.doubleValue();
            int exponent = (int) Math.floor(Math.log10(doubleValue));
            return formatLargeNumberUsingExponent(BigDecimal.valueOf(doubleValue), exponent);
        }
    }


    private String formatLargeNumberUsingExponent(BigDecimal value, int exponent) {
        String[] largeNumberNames = {
                "thousand", "million", "billion", "trillion", "quadrillion", "quintillion", "sextillion",
                "septillion", "octillion", "nonillion", "decillion", "undecillion", "duodecillion",
                "tredecillion", "quattuordecillion"
        };

        int index = (exponent - 3) / 3;

        if (index < 0) {
            return numberFormat.format(value);
        } else if (index < largeNumberNames.length) {
            BigDecimal divisor = BigDecimal.valueOf(Math.pow(10, index * 3 + 3));
            BigDecimal shortNumber = value.divide(divisor, 1, RoundingMode.HALF_UP);
            return String.format("%.1f %s", shortNumber, largeNumberNames[index]);
        } else {
            return String.format("%.1e", value);
        }
    }




    public String getFormattedString(int frameCount, int updateFrequency, String delimiter) {
        if (frameCount - lastUpdateFrame >= updateFrequency || cachedFormattedString.isEmpty()) {
            StringBuilder formattedString = new StringBuilder();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object value = entry.getValue();
                String formattedValue;

                if (value instanceof Number && ((Number) value).doubleValue() >= Math.pow(10, 9)) {
                    formattedValue = formatLargeNumber(value);
                } else if (value instanceof BigInteger && ((BigInteger) value).compareTo(BigInteger.valueOf(1000000000)) >= 0) {
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

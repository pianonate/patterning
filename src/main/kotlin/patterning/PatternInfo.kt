package patterning;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatternInfo {

    private final Map<String, Object> data; // Changed from Number to Object

    PatternInfo() {
        data = new LinkedHashMap<>(); // Use LinkedHashMap to maintain the insertion order
    }

    public void addOrUpdate(String key, Object value) {
        if (value instanceof Number) {
            data.put(key, value);
        } else {
            throw new IllegalArgumentException("Value must be a Number or BigInteger.");
        }
    }

    public void addOrUpdate(String key, String value) {
        data.put(key, value);
    }

    public Number get(String key) {
        return (Number) data.get(key);
    }
}

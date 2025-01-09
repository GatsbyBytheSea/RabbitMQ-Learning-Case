package edu.imtilsd.rabbimq_case;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        // Attempt to load application.properties from the resources directory.
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("application.properties not found on classpath!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve Configuration
     */
    public static String get(String key) {
        return props.getProperty(key);
    }

    /**
     * Retrieve int-type configuration
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

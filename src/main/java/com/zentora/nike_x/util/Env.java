package com.zentora.nike_x.util;

import java.io.InputStream;
import java.util.Properties;
import io.github.cdimascio.dotenv.Dotenv;

public class Env {
    private static final Properties APP_PROPERTIES = new Properties();
    private static Dotenv dotenv;

    static {
        try {
            InputStream inputStream = Env.class.getClassLoader().getResourceAsStream("app.properties");
            if (inputStream != null) {
                APP_PROPERTIES.load(inputStream);
            }

            // Load environment variables (.env file or system env)
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
        } catch (Exception e) {
            throw new RuntimeException("Environment variables or properties loading failed: " + e.getMessage());
        }
    }

    public static String get(String key) {
        // Check Dotenv (which also checks System.getenv) first
        String value = dotenv.get(key);
        if (value != null) {
            return value;
        }
        // Fallback to app.properties
        return APP_PROPERTIES.getProperty(key);
    }

    public static Properties getAppProperties() {
        return APP_PROPERTIES;
    }
}
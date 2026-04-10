package com.tanksqa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads framework settings from src/test/resources/config.properties.
 *
 * All hardcoded values (host, port, timeouts) live in that file.
 * Change a value there — no Java code needs to recompile.
 *
 * This class is final and has a private constructor because it is a
 * utility class — you call its static methods directly, you never
 * create an instance of it.
 */
public final class Config {

    private static final Properties PROPS = new Properties();

    // This block runs once when the class is first used.
    // If config.properties is missing, we fail immediately with a clear message
    // rather than getting a confusing NullPointerException later.
    static {
        try (InputStream in = Config.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new RuntimeException(
                    "config.properties not found. Expected at src/test/resources/config.properties");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not read config.properties: " + e.getMessage());
        }
    }

    private Config() { }

    public static String bridgeHost() {
        return PROPS.getProperty("bridge.host", "127.0.0.1");
    }

    public static int bridgePort() {
        return Integer.parseInt(PROPS.getProperty("bridge.port", "13000"));
    }

    public static long waitTimeoutMs() {
        return Long.parseLong(PROPS.getProperty("wait.timeout.ms", "10000"));
    }

    public static long waitIntervalMs() {
        return Long.parseLong(PROPS.getProperty("wait.interval.ms", "500"));
    }
}

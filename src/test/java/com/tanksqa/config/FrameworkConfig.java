package com.tanksqa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads framework configuration from src/test/resources/config.properties.
 *
 * Every configurable value lives here. Tests and framework classes read from
 * this class — never from hardcoded literals. To change a value, edit
 * config.properties; no Java code needs to change.
 */
public final class FrameworkConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = FrameworkConfig.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new ExceptionInInitializerError(
                    "config.properties not found on the classpath. " +
                    "Expected at src/test/resources/config.properties");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                "Failed to load config.properties: " + e.getMessage());
        }
    }

    private FrameworkConfig() { }

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

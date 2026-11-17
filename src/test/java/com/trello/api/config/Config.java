package com.trello.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Reads settings from environment variables first (TRELLO_KEY), then config.properties (trello.key). */
public final class Config {
    private static final Properties FILE = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                FILE.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Config() { }

    public static String get(String key) {
        String env = System.getenv(key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String value = FILE.getProperty(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public static String require(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException("Missing setting '" + key + "'. Set env var "
                    + key.toUpperCase().replace('.', '_')
                    + " or add it to src/test/resources/config.properties (see config.properties).");
        }
        return value;
    }

    public static String baseUri() {
        String value = get("trello.base.uri");
        return value != null ? value : "https://api.trello.com";
    }
}

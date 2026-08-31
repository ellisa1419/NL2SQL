package com.acme.nl2sql.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application settings, loaded from {@code application.properties} on the classpath.
 *
 * <p>Any key may be overridden on the command line with a matching system property,
 * for example {@code -Dserver.port=9090}.
 */
public final class EngineConfig {

    private static final String RESOURCE = "application.properties";

    private final Properties props;

    private EngineConfig(Properties props) {
        this.props = props;
    }

    /**
     * Reads the bundled properties file.
     *
     * @return the resolved configuration
     * @throws IllegalStateException if the properties file is missing or unreadable
     */
    public static EngineConfig load() {
        Properties p = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing classpath resource: " + RESOURCE);
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + RESOURCE, e);
        }
        return new EngineConfig(p);
    }

    /**
     * Looks up a string setting.
     *
     * @param key          property name
     * @param defaultValue value to use when neither a system property nor the file supplies one
     * @return the resolved value
     */
    public String get(String key, String defaultValue) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return props.getProperty(key, defaultValue);
    }

    /**
     * Looks up a numeric setting.
     *
     * @param key          property name
     * @param defaultValue value to use when the setting is absent
     * @return the resolved value
     * @throws IllegalStateException if the configured value is not a number
     */
    public long getLong(String key, long defaultValue) {
        String raw = get(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(key + " is not a number: " + raw, e);
        }
    }

    /**
     * Looks up an integer setting.
     *
     * @param key          property name
     * @param defaultValue value to use when the setting is absent
     * @return the resolved value
     */
    public int getInt(String key, int defaultValue) {
        return (int) getLong(key, defaultValue);
    }
}

package cltool4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

/**
 * Represents configuration properties for a command-line tool. Generally accessed at runtime via the
 * singleton subclass {@link GlobalConfigProperties}. This implementation allows programmatic configuration as
 * well (e.g., for unit testing).
 * 
 * @author Aaron Dunlop
 * @since Oct 2010
 */
public class ConfigProperties extends Properties {

    public ConfigProperties() {
    }

    /**
     * Returns the value of the specified property
     * 
     * @param key Key
     * @return the value of the specified property
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an integer
     */
    @Override
    public String getProperty(final String key) {
        final String value = super.getProperty(key);
        if (value == null) {
            throw new InvalidConfigurationException(key);
        }
        return value;
    }

    /**
     * Returns the value of the specified property
     * 
     * @param key Key
     * @param defaultValue Default value (returned if <code>key</code> is not set)
     * @return the value of the specified property or <code>defaultValue</code> if not set
     */
    @Override
    public String getProperty(final String key, final String defaultValue) {
        final String value = super.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Parses the specified property as an integer.
     * 
     * @param key Key
     * @return the specified property as an integer
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an integer
     */
    public int getIntProperty(final String key) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (final NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Parses the specified property as an integer.
     * 
     * @param key Key
     * @param defaultValue Default value (returned if <code>key</code> is not set)
     * @return the value of the specified property or <code>defaultValue</code> if not set
     */
    public int getIntProperty(final String key, final int defaultValue) {
        if (!containsKey(key)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(getProperty(key));
        } catch (final NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Parses the specified property as an float.
     * 
     * @param key Key
     * @return the specified property as an float
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an float
     */
    public float getFloatProperty(final String key) {
        try {
            return Float.parseFloat(getProperty(key));
        } catch (final NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Parses the specified property as a float.
     * 
     * @param key Key
     * @param defaultValue Default value (returned if <code>key</code> is not set)
     * @return the value of the specified property or <code>defaultValue</code> if not set
     */
    public float getFloatProperty(final String key, final float defaultValue) {
        if (!containsKey(key)) {
            return defaultValue;
        }

        try {
            return Float.parseFloat(getProperty(key));
        } catch (final NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Parses the specified property as an boolean.
     * 
     * @param key Key
     * @return the specified property as an boolean
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an boolean
     */
    public boolean getBooleanProperty(final String key) {
        try {
            return Boolean.parseBoolean(getProperty(key));
        } catch (final NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Parses the specified property as a float.
     * 
     * @param key Key
     * @param defaultValue Default value (returned if <code>key</code> is not set)
     * @return the value of the specified property or <code>defaultValue</code> if not set
     */
    public boolean getBooleanProperty(final String key, final boolean defaultValue) {
        if (!containsKey(key)) {
            return defaultValue;
        }

        try {
            return Boolean.parseBoolean(getProperty(key));
        } catch (final NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Merges the provided properties into global property storage, overwriting any conflicting keys (that is,
     * properties set in the provided {@link Properties} instance override those in the current global
     * storage).
     * 
     * @param newProperties New {@link Properties}
     */
    public void mergeOver(final Properties newProperties) {
        for (final Object key : newProperties.keySet()) {
            setProperty((String) key, newProperties.getProperty((String) key));
        }
    }

    /**
     * Merges the provided properties into global property storage, skipping any conflicting keys (that is,
     * existing properties override properties set in the provided {@link Properties} instance).
     * 
     * @param newProperties new {@link Properties}
     */
    public void mergeUnder(final Properties newProperties) {
        for (final Object key : newProperties.keySet()) {
            if (!containsKey(key)) {
                setProperty((String) key, newProperties.getProperty((String) key));
            }
        }
    }

    @Override
    public String toString() {
        final ArrayList<String> stringProps = new ArrayList<String>();
        for (final Object key : keySet()) {
            stringProps.add((String) key + "=" + getProperty((String) key) + '\n');
        }
        Collections.sort(stringProps);

        final StringBuilder sb = new StringBuilder(128);
        for (final String property : stringProps) {
            sb.append(property);
        }
        // Remove final line feed
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Thrown if an unknown property is requested from {@link ConfigProperties}. Clients of
     * {@link ConfigProperties} generally expect any required configuration parameters to be specified, and
     * {@link BaseCommandlineTool} handles the exception case if they are not.
     */
    public static class InvalidConfigurationException extends RuntimeException {

        public InvalidConfigurationException(final String key) {
            super("No value found for configuration option " + key);
        }

        public InvalidConfigurationException(final String key, final String message) {
            super("Invalid configuration option " + key + " : " + message);
        }
    }

}

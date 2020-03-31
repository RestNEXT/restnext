/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.restnext.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of utility methods to retrieve and parse the values of the Java system properties.
 */
public final class SystemPropertyUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(SystemPropertyUtils.class);

  /**
   * Check if contains this key.
   *
   * @param key the key
   * @return {@code true} if and only if the system property with the specified {@code key} exists
   */
  public static boolean contains(String key) {
    return get(key) != null;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to {@code null} if the property access fails.
   *
   * @param key the key
   * @return the property value or {@code null}
   */
  public static String get(String key) {
    return get(key, null);
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if the property access fails.
   *
   * @param key the key
   * @param def the default value
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static String get(final String key, String def) {
    Objects.requireNonNull(key, "key");
    if (key.isEmpty()) {
      throw new IllegalArgumentException("key must not be empty.");
    }

    String value = null;
    try {
      if (System.getSecurityManager() == null) {
        value = System.getProperty(key);
      } else {
        value = AccessController.doPrivileged(new PrivilegedAction<String>() {
          @Override
          public String run() {
            return System.getProperty(key);
          }
        });
      }
    } catch (SecurityException e) {
      LOGGER.warn(
          "Unable to retrieve a system property '{}'; default values will be used.", key, e);
    }

    if (value == null) {
      return def;
    }

    return value;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if
   * the property access fails.
   *
   * @param key the key
   * @param def the default value
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static boolean getBoolean(String key, boolean def) {
    String value = get(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase(Locale.getDefault());
    if (value.isEmpty()) {
      return def;
    }

    if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
      return true;
    }

    if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
      return false;
    }

    LOGGER.warn(
        "Unable to parse the boolean system property '{}':{} - using the default value: {}",
        key, value, def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if
   * the property access fails.
   *
   * @param key the key
   * @param def the default value
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static int getInt(String key, int def) {
    String value = get(key);
    if (value == null) {
      return def;
    }

    value = value.trim();
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignore) {
      LOGGER.warn("Invalid long value");
    }

    LOGGER.warn(
        "Unable to parse the integer system property '{}':{} - using the default value: {}",
        key, value, def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if
   * the property access fails.
   *
   * @param key the key
   * @param def the default value
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static long getLong(String key, long def) {
    String value = get(key);
    if (value == null) {
      return def;
    }

    value = value.trim();
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignore) {
      LOGGER.warn("Invalid long value");
    }

    LOGGER.warn(
        "Unable to parse the long integer system property '{}':{} - using the default value: {}",
        key, value, def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified {@code key}, while falling
   * back to {@code null} if the property access fails.
   *
   * @param key the key
   * @param more additional strings to be joined to form the path string
   * @return the property value or {@code null}
   */
  public static Path getPath(String key, String... more) {
    return getPath(key, null, more);
  }

  /**
   * Returns the value of the Java system property with the specified {@code key}, while falling
   * back to the specified default value if the property access fails.
   *
   * @param key the key
   * @param def the default value
   * @param more additional strings to be joined to form the path string
   * @return the property value. {@code def} if there's no such property or if an access to the
   *     specified property is not allowed.
   */
  public static Path getPath(String key, Path def, String... more) {
    String value = get(key);
    return value != null ? Paths.get(value, more) : def;
  }

  private SystemPropertyUtils() {
    // Unused
  }

}

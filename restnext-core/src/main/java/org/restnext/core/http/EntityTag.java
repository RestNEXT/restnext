/*
 * Copyright (C) 2016 Thiago Gutenberg Carvalho da Costa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.restnext.core.http;

import java.util.Objects;

/**
 * An abstraction for the value of a HTTP Entity Tag, used as the value
 * of an ETag response header.
 *
 * @author Paul Sandoz
 * @author Marc Hadley
 * @author Thiago Gutenberg Carvalho da Costa
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.11">HTTP/1.1 section
 * 3.11</a>
 * @since 1.0
 */
public class EntityTag {

  /**
   * An EntityTag that corresponds to {@code If-Match: *} or {@code If-None-Match: *}.
   *
   * <p>Note that this type cannot be used to create request header values for
   * {@code If-Match} and {@code If-None-Match} of the form
   * {@code If-Match: *} or {@code If-None-Match: *} as
   * {@code *} is not a valid entity tag.
   */
  public static final EntityTag ANY_MATCH = new EntityTag("*");

  private String value;
  private boolean weak;

  /**
   * Creates a new instance of a strong {@code EntityTag}.
   *
   * @param value the value of the tag, quotes not included.
   * @throws NullPointerException if value is {@code null}.
   */
  public EntityTag(final String value) {
    this(value, false);
  }

  /**
   * Creates a new instance of an {@code EntityTag}.
   *
   * @param value the value of the tag, quotes not included.
   * @param weak  {@code true} if this represents a weak tag, {@code false} otherwise.
   * @throws NullPointerException if value is {@code null}.
   */
  public EntityTag(final String value, final boolean weak) {
    this.value = Objects.requireNonNull(value, "value");
    this.weak = weak;
  }

  /**
   * Creates a new instance of {@code EntityTag} by parsing the supplied string.
   *
   * @param value the entity tag string.
   * @return the newly created entity tag.
   * @throws RuntimeException if the supplied string cannot be parsed or is {@code null}.
   */
  public static EntityTag valueOf(String value) {
    Objects.requireNonNull(value, "value");

    boolean isWeak = value.startsWith("W/\"") && value.endsWith("\"");
    boolean isQuoted = (isWeak || value.startsWith("\"")) && value.endsWith("\"");
    boolean isAnyMatch = "*".equals(value);

    // check if is eTag quoted value header... W/""sdkjhd"" or ""sdkjhd""
    if (isQuoted) {
      // removes the first and the last quoted - ""sdkjhd"" -> "sdkjhd"
      value = value.substring(value.indexOf("\"") + 1, value.lastIndexOf("\""));
      // removes the escaped quoted in the eTag value for non escaped quoted - "sdkjhd\""->"sdkjhd""
      value = value.replaceAll("\\\\\"", "\"");
    }

    if (isAnyMatch) {
      return EntityTag.ANY_MATCH;
    }

    if (isWeak) {
      return new EntityTag(value, true);
    }

    if (!isQuoted) {
      throw new RuntimeException(String.format(
          "header value (%s) is not a valid entity tag.", value));
    }

    return new EntityTag(value);
  }

  /**
   * Generate hashCode based on value and weakness.
   *
   * @return the entity tag hash code.
   */
  @Override
  public int hashCode() {
    return Objects.hash(value, weak);
  }

  /**
   * Compares {@code obj} to this tag to see if they are the same considering
   * weakness and value.
   *
   * @param o the object to compare to.
   * @return {@code true} if the two tags are the same, {@code false} otherwise.
   */
  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityTag)) {
      return false;
    }
    EntityTag other = (EntityTag) o;
    return Objects.equals(value, other.value) && weak == other.weak;
  }

  /**
   * Get the value of an {@code EntityTag}.
   *
   * @return the value of the tag.
   */
  public String getValue() {
    return value;
  }

  /**
   * Check the strength of an {@code EntityTag}.
   *
   * @return {@code true} if this represents a weak tag, {@code false} otherwise.
   */
  public boolean isWeak() {
    return weak;
  }

  /**
   * Convert the entity tag to a string suitable for use as the value of the
   * corresponding HTTP header.
   *
   * @return a string version of the entity tag.
   */
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    if (weak) {
      b.append("W/");
    }
    appendQuoted(b, value);
    //b.append('"').append(value.replaceAll("\"", "\\\\\"")).append('"');
    return b.toString();
  }

  /**
   * Append a new quoted value to the string builder.
   *
   * <p>The appended value is quoted and all the quotes in the value are escaped.
   *
   * @param b     string builder to be updated.
   * @param value value to be appended.
   */
  public static void appendQuoted(StringBuilder b, String value) {
    b.append('"');
    appendEscapingQuotes(b, value);
    b.append('"');
  }

  /**
   * Append a new value to the string builder.
   *
   * <p>All the quotes in the value are escaped before appending.
   *
   * @param b     string builder to be updated.
   * @param value value to be appended.
   */
  public static void appendEscapingQuotes(StringBuilder b, String value) {
    for (int i = 0; i < value.length(); i++) {
      char current = value.charAt(i);
      if (current == '"') {
        b.append('\\');
      }
      b.append(current);
    }
  }

}

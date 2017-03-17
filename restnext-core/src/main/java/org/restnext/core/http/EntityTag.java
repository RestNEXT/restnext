/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
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
    Objects.requireNonNull(value, "value must not be null");
    this.value = value;
    this.weak = weak;
  }

  /**
   * Creates a new instance of {@code EntityTag} by parsing the supplied string.
   *
   * @param value the entity tag string.
   * @return the newly created entity tag.
   * @throws NullPointerException if the supplied string cannot be parsed or is {@code null}.
   */
  public static EntityTag fromString(String value) {
    Objects.requireNonNull(value, "value must not be null");

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

    if (!isQuoted && !isAnyMatch) {
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
    int hash = 3;
    hash = 17 * hash + (this.value != null ? this.value.hashCode() : 0);
    hash = 17 * hash + (this.weak ? 1 : 0);
    return hash;
  }

  /**
   * Compares {@code obj} to this tag to see if they are the same considering
   * weakness and value.
   *
   * @param obj the object to compare to.
   * @return {@code true} if the two tags are the same, {@code false} otherwise.
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof EntityTag)) {
      return super.equals(obj);
    }
    EntityTag other = (EntityTag) obj;
    return value.equals(other.getValue()) && weak == other.isWeak();
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

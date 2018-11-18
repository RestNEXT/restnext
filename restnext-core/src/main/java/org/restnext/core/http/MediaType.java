/*
 * Copyright (C) 2013 Square, Inc.
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

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * An <a href="http://tools.ietf.org/html/rfc2045">RFC 2045</a> Media Type, appropriate to describe
 * the content type of an HTTP request or response body.
 */
public final class MediaType {

  private static final String TYPE_WILDCARD = "*";

  private static final String TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)";
  private static final String QUOTED = "\"([^\"]*)\"";
  private static final Pattern TYPE_SUBTYPE = Pattern.compile(TOKEN + "/" + TOKEN);
  private static final Pattern PARAMETER = Pattern.compile(
      ";\\s*(?:" + TOKEN + "=(?:" + TOKEN + "|" + QUOTED + "))?");

  public static final MediaType WILDCARD = MediaType.parse("*/*");
  public static final MediaType TEXT = MediaType.parse("text/plain");
  public static final MediaType TEXT_UTF8 = MediaType.parse(TEXT.mediaType + ";charset=utf-8");

  private final String mediaType;
  private final String type;
  private final String subtype;
  private final @Nullable String charset;

  private MediaType(String mediaType, String type, String subtype, @Nullable String charset) {
    this.mediaType = mediaType;
    this.type = type;
    this.subtype = subtype;
    this.charset = charset;
  }

  public static MediaType valueOf(String string) {
    return parse(string);
  }

  /**
   * Returns a media type for {@code string}, or null if {@code string} is not a well-formed media
   * type.
   *
   * @param string media type string
   * @return media type
   */
  public static @Nullable MediaType parse(String string) {
    try {
      return get(string);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  /**
   * Returns a media type for {@code string}.
   *
   * @param string media type string
   * @return media type
   * @throws IllegalArgumentException if {@code string} is not a well-formed media type.
   */
  public static MediaType get(String string) {
    Matcher typeSubtype = TYPE_SUBTYPE.matcher(string);
    if (!typeSubtype.lookingAt()) {
      throw new IllegalArgumentException("No subtype found for: \"" + string + '"');
    }
    String type = typeSubtype.group(1).toLowerCase(Locale.US);
    String subtype = typeSubtype.group(2).toLowerCase(Locale.US);

    String charset = null;
    Matcher parameter = PARAMETER.matcher(string);
    for (int s = typeSubtype.end(); s < string.length(); s = parameter.end()) {
      parameter.region(s, string.length());
      if (!parameter.lookingAt()) {
        throw new IllegalArgumentException("Parameter is not formatted correctly: \""
            + string.substring(s)
            + "\" for: \""
            + string
            + '"');
      }

      String name = parameter.group(1);
      if (name == null || !name.equalsIgnoreCase("charset")) {
        continue;
      }
      String charsetParameter;
      String token = parameter.group(2);
      if (token != null) {
        // If the token is 'single-quoted' it's invalid! But we're lenient and strip the quotes.
        charsetParameter = token.startsWith("'") && token.endsWith("'") && token.length() > 2
            ? token.substring(1, token.length() - 1)
            : token;
      } else {
        // Value is "double-quoted". That's valid and our regex group already strips the quotes.
        charsetParameter = parameter.group(3);
      }
      if (charset != null && !charsetParameter.equalsIgnoreCase(charset)) {
        throw new IllegalArgumentException("Multiple charsets defined: \""
            + charset
            + "\" and: \""
            + charsetParameter
            + "\" for: \""
            + string
            + '"');
      }
      charset = charsetParameter;
    }

    return new MediaType(string, type, subtype, charset);
  }

  /**
   * Returns the high-level media type, such as "text", "image", "audio", "video", or
   * "application".
   *
   * @return type
   */
  public String type() {
    return type;
  }

  /**
   * Returns a specific media subtype, such as "plain" or "png", "mpeg", "mp4" or "xml".
   *
   * @return subtype
   */
  public String subtype() {
    return subtype;
  }

  /**
   * Returns the charset of this media type, or null if this media type doesn't specify a charset.
   *
   * @return charset
   */
  public @Nullable Charset charset() {
    return charset(null);
  }

  /**
   * Returns the charset of this media type, or {@code defaultValue} if either this media type
   * doesn't specify a charset, of it its charset is unsupported by the current runtime.
   *
   * @param defaultValue the default charset
   * @return charset
   */
  public @Nullable Charset charset(@Nullable Charset defaultValue) {
    try {
      return charset != null ? Charset.forName(charset) : defaultValue;
    } catch (IllegalArgumentException e) {
      return defaultValue; // This charset is invalid or unsupported. Give up.
    }
  }

  /**
   * Check if this media type is compatible with another media type. E.g.
   * image/* is compatible with image/jpeg, image/png, etc. Media type
   * parameters are ignored. The function is commutative.
   *
   * @param other the media type to compare with.
   * @return true if the types are compatible, false otherwise.
   */
  public boolean isCompatible(MediaType other) {
    return other != null
        && // return false if other is null, else
        ((type.equals(TYPE_WILDCARD) || other.type.equals(TYPE_WILDCARD))
            && (subtype.equals(TYPE_WILDCARD) || other.subtype.equals(TYPE_WILDCARD))
            || // both are wildcard types and wildcard subtypes, or
            type.equalsIgnoreCase(other.type) && (subtype.equals(TYPE_WILDCARD)
                || other.subtype.equals(TYPE_WILDCARD))
            || // same types, wildcard sub-types, or
            type.equalsIgnoreCase(other.type) && subtype.equalsIgnoreCase(other.subtype));
    // same types & sub-types
  }

  /**
   * Check if this media type is similar with another media type. E.g.
   * application/xml;q=0.9 is similar with application/xml, application/xml;charset=utf-8, etc.
   * Media type parameters are ignored.
   *
   * @param other the media type to compare with.
   * @return true if the types are similar, false otherwise.
   */
  public boolean isSimilar(MediaType other) {
    return other != null && type.equals(other.type) && subtype.equals(other.subtype);
  }

  /**
   * Returns the encoded media type, like "text/plain; charset=utf-8", appropriate for use in a
   * Content-Type header.
   *
   * @return media type as string
   */
  @Override
  public String toString() {
    return mediaType;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof MediaType && ((MediaType) other).mediaType.equals(mediaType);
  }

  @Override
  public int hashCode() {
    return mediaType.hashCode();
  }
}

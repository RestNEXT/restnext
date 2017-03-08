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

package org.restnext.util;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by thiago on 10/18/16.
 */
public final class UriUtils {

  /**
   * Regex to validate uris with or without parameters.
   * E.g: /test/1/{id}/sub/{2}/{name}
   */
  public static final Pattern PATH_PARAM_URI = Pattern.compile(
      "^([/])(([/\\w])+(/\\{[\\w]+\\})*)*([?])?$");

  private UriUtils() {
    throw new AssertionError();
  }

  /**
   * Normalize the uri.
   *
   * <p>Add the first slash;
   *
   * <p>Remove the last slash;
   *
   * @param uri the uri
   * @return the normalized uri
   */
  public static String normalize(String uri) {
    return Optional.ofNullable(uri)
        .map(UriUtils::addFirstSlash)
        .map(UriUtils::removeLastSlash)
        .orElse(uri);
  }

  /**
   * Add first slash in the uri if not exists.
   *
   * @param uri the uri
   * @return the uri with first slash added
   */
  public static String addFirstSlash(String uri) {
    return Optional.ofNullable(uri)
        .filter(u -> u.length() >= 1 && u.charAt(0) != '/')
        .map(u -> "/" + u)
        .orElse(uri);
  }

  /**
   * Remove the last slash from the uri.
   *
   * @param uri the uri
   * @return the uri without the last slash
   */
  public static String removeLastSlash(String uri) {
    return Optional.ofNullable(uri)
        .filter(u -> u.length() > 1 && u.lastIndexOf('/') == (u.length() - 1))
        .map(u -> u.substring(0, u.lastIndexOf('/')))
        .orElse(uri);
  }

  public static boolean isPathParamUri(final String uri) {
    return uri != null && PATH_PARAM_URI.matcher(uri).matches();
  }
}

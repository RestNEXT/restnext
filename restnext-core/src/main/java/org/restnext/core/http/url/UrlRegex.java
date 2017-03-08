/*
    Copyright 2011, Strategic Gains, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package org.restnext.core.http.url;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL regex.
 *
 * @author toddf
 * @since Jan 7, 2011
 */
public class UrlRegex implements UrlMatcher {

  public static final String PARAMETER_PREFIX = "regexGroup";

  private Pattern pattern;

  public UrlRegex(String regex) {
    this(Pattern.compile(regex));
  }

  public UrlRegex(Pattern pattern) {
    super();
    setPattern(pattern);
  }

  @Override
  public boolean matches(String url) {
    return (match(url) != null);
  }

  @Override
  public UrlMatch match(String url) {
    Matcher matcher = pattern.matcher(url);

    if (matcher.matches()) {
      return new UrlMatch(extractParameters(matcher));
    }

    return null;
  }

  public String getPattern() {
    return pattern.pattern();
  }

  private void setPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public List<String> getParameterNames() {
    return null;
  }

  @Override
  public String toRegexPattern(String url) {
    // do not modify the uri, since the caller is building their own regex and is ON THEIR OWN...
    return url;
  }

  /**
   * Extracts parameter values from a Matcher instance.
   *
   * @param matcher matcher
   * @return a Map containing parameter values indexed by their corresponding parameter name.
   */
  private Map<String, String> extractParameters(Matcher matcher) {
    Map<String, String> values = new HashMap<>();

    for (int i = 0; i < matcher.groupCount(); i++) {
      String value = matcher.group(i + 1);

      if (value != null) {
        values.put(PARAMETER_PREFIX + i, value);
      }
    }

    return values;
  }
}
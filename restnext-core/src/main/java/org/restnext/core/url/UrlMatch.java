/*
 * Copyright 2010, Strategic Gains, Inc.
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

package org.restnext.core.url;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Contains the results of a UrlPattern.match() call, reflecting the match outcome
 * and containing any parameter values, if applicable.
 *
 * <p>UrlMatch is immutable.
 *
 * @author toddf
 * @since Apr 29, 2010
 */
public class UrlMatch {

  /**
   * Parameter values parsed from the URL during the match.
   */
  private Map<String, String> parameters = new HashMap<>();


  // SECTION: CONSTRUCTOR

  /**
   * Constructor with map parameters.
   *
   * @param parameters parameters
   */
  public UrlMatch(Map<String, String> parameters) {
    super();

    if (parameters != null) {
      this.parameters.putAll(parameters);
    }
  }

  // SECTION: ACCESSORS

  /**
   * Retrieves a parameter value parsed from the URL during the match.
   *
   * @param name the name of a parameter for which to retrieve the value.
   * @return the parameter value from the URL, or null if not present.
   */
  public String get(String name) {
    return parameters.get(name);
  }

  /**
   * Retrieves the parameter entries as a set.
   *
   * @return a Set of Map entries (by String, String).
   */
  public Set<Entry<String, String>> parameterSet() {
    return Collections.unmodifiableSet(parameters.entrySet());
  }
}

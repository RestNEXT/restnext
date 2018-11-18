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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Url Pattern Test.
 *
 * @author toddf
 * @since Apr 28, 2010
 */
public class UrlPatternTest {
  private final UrlMatcher pattern = new UrlPattern("/xxx/{a_id}/yyy/{b_id}");
  private final UrlMatcher patternFormat = new UrlPattern("/xxx/{a_id}/yyy/{b_id}.{format}");

  @Test
  public void shouldMatchUrlWithFormat() {
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/joez.json"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/joez."));
    assertTrue(patternFormat.matches("/xxx/todd.fredrich/yyy/joez.json"));
    assertTrue(patternFormat.matches("/xxx/[toddf]:1/yyy/joez.json"));
    assertTrue(patternFormat.matches("/xxx/12345/yyy/67890.json"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/joez.xml"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/joez.json?x=y&a=b"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/joez"));
    assertTrue(patternFormat.matches("/xxx/v1_0_1/yyy/yada"));
    assertTrue(patternFormat.matches("/xxx/v1_0_1/yyy/yada.json"));
    assertTrue(patternFormat.matches("/xxx/v1_0_1/yyy/yada.json?x=y&a=b"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/jose.js%20on"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/jose.%20json"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/jose.%json"));
    assertTrue(patternFormat.matches("/xxx/$-_@&+-[]/yyy/!*'(),.json"));
    assertTrue(patternFormat.matches("/xxx/toddf/yyy/.json"));
    assertTrue(patternFormat.matches("/xxx/.toddf/yyy/.json?a=foo"));
  }

  @Test
  public void shouldMatchUrlWithoutFormat() {
    assertTrue(pattern.matches("/xxx/toddf/yyy/joez"));
    assertTrue(pattern.matches("/xxx/toddf/yyy/joez."));
    assertTrue(pattern.matches("/xxx/[toddf]:1/yyy/joez"));
    assertTrue(pattern.matches("/xxx/todd.fredrich/yyy/joez"));
    assertTrue(pattern.matches("/xxx/12345/yyy/67890"));
    assertTrue(pattern.matches("/xxx/toddf/yyy/joez"));
    assertTrue(pattern.matches("/xxx/toddf/yyy/joez?x=y&a=b"));
    assertTrue(pattern.matches("/xxx/toddf/yyy/.json"));
    assertTrue(pattern.matches("/xxx/.toddf/yyy/.json"));
  }

  @Test
  public void shouldNotMatchUrlWithFormat() {
    assertFalse(patternFormat.matches("/xxx/toddf/yyy/?a=foo"));
    assertFalse(patternFormat.matches("/xxx/toddf/yyy/joez/"));
    assertFalse(patternFormat.matches("/xxx/todd.fredrich/yyy/joez/"));
    assertFalse(patternFormat.matches("/aaa/toddf/yyy/joez.json"));
    assertFalse(patternFormat.matches("/xxx/toddf/bbb/joez.json"));
    assertFalse(patternFormat.matches("/xxx/toddf/yyy/"));
    assertFalse(patternFormat.matches("/xxx/toddf/yyy"));
  }

  @Test
  public void shouldNotMatchUrlWithoutFormat() {
    assertFalse(pattern.matches("/xxx/toddf/yyy/joez/"));
    assertFalse(pattern.matches("/aaa/toddf/yyy/joez.json"));
    assertFalse(pattern.matches("/xxx/toddf/bbb/joez.json"));
    assertFalse(pattern.matches("/xxx/toddf/yyy/"));
    assertFalse(pattern.matches("/xxx/toddf/yyy"));
  }

  @Test
  public void shouldParseParametersWithFormat() {
    UrlMatch match = patternFormat.match("/xxx/12345/yyy/67890.json");
    assertNotNull(match);
    assertEquals("json", match.get("format"));
    assertEquals("12345", match.get("a_id"));
    assertEquals("67890", match.get("b_id"));
  }

  @Test
  public void shouldParseSpecialParametersWithFormat() {
    UrlMatch match = patternFormat.match("/xxx/~,;=$-_@&+-[toddf]:12345/yyy/!*'(fredt),.json");
    assertNotNull(match);
    assertEquals("json", match.get("format"));
    assertEquals("~,;=$-_@&+-[toddf]:12345", match.get("a_id"));
    assertEquals("!*'(fredt),", match.get("b_id"));
  }

  @Test
  public void shouldParseParametersWithoutFormat() {
    UrlMatch match = pattern.match("/xxx/12345/yyy/67890");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("12345", match.get("a_id"));
    assertEquals("67890", match.get("b_id"));
  }

  @Test
  public void shouldParseSpecialParametersWithoutFormat() {
    UrlMatch match = pattern.match("/xxx/~,;=$-_@&+-[toddf]:12345/yyy/!*'(fredt),");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("~,;=$-_@&+-[toddf]:12345", match.get("a_id"));
    assertEquals("!*'(fredt),", match.get("b_id"));
  }

  @Test
  public void shouldParseParametersWithQueryStringAndFormat() {
    UrlMatch match = patternFormat.match("/xxx/12345/yyy/67890.js_on?x=y&a=b");
    assertNotNull(match);
    assertEquals("js_on", match.get("format"));
    assertEquals("12345", match.get("a_id"));
    assertEquals("67890", match.get("b_id"));

    match = patternFormat.match("/xxx/12.345/yyy/678.90.json?x=y&a=b");
    assertNotNull(match);
    assertEquals("json", match.get("format"));
    assertEquals("12.345", match.get("a_id"));
    assertEquals("678.90", match.get("b_id"));

    match = patternFormat.match("/xxx/12.345/yyy/678.90.?x=y&a=b");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("12.345", match.get("a_id"));
    assertEquals("678.90.", match.get("b_id"));
  }

  @Test
  public void shouldParseParametersWithQueryStringAndWithoutFormat() {
    UrlMatch match = pattern.match("/xxx/12345/yyy/67890?x=y&a=b");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("12345", match.get("a_id"));
    assertEquals("67890", match.get("b_id"));

    match = pattern.match("/xxx/12.345/yyy/678.90?x=y&a=b");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("12.345", match.get("a_id"));
    assertEquals("678.90", match.get("b_id"));
  }

  @Test
  public void shouldMatchUrlWithQuestionMark() {
    UrlMatch match = pattern.match("/xxx/12345/yyy/67890?");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("12345", match.get("a_id"));
    assertEquals("67890", match.get("b_id"));

    match = pattern.match("/xxx/12.345/yyy/678.90?");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("12.345", match.get("a_id"));
    assertEquals("678.90", match.get("b_id"));
  }

  @Test
  public void shouldMatchUrlWithQuestionMarkAndFormat() {
    UrlMatch match = patternFormat.match("/xxx/12345/yyy/67890.json?");
    assertNotNull(match);
    assertEquals("json", match.get("format"));
    assertEquals("12345", match.get("a_id"));
    assertEquals("67890", match.get("b_id"));

    match = patternFormat.match("/xxx/12.345/yyy/678.90.json?");
    assertNotNull(match);
    assertEquals("json", match.get("format"));
    assertEquals("12.345", match.get("a_id"));
    assertEquals("678.90", match.get("b_id"));
  }

  @Test
  public void shouldParseParametersWithPeriods() {
    UrlMatch match = pattern.match("/xxx/1.0/yyy/67890?x=y&a=b");
    assertNotNull(match);
    assertNull(match.get("format"));
    assertEquals("1.0", match.get("a_id"));
    assertEquals("67890", match.get("b_id"));
  }

  @Test
  public void shouldParseTimestampParameters() {
    UrlMatcher huge = new UrlPattern(
        "/name/{ORDER}/{NO_OF_RECORDS}/{SIFREF_ID}/{REQUEST_TIME_STAMP}/{KEY_MONIKER}/"
            + "{AUTHENTICATION_TOKEN}");
    UrlMatch match = huge.match("/name/asc/4/2A3B4C5E6D/12345/foo/"
        + "560261e05f7c21533a3d7d09efff4b91eb7ca702f490e073e1d51614c2e33c1e"
        + "?contenttype=jsonp%26jsonp=_jqjsp%26_1287161495587");
    assertNotNull(match);
    assertEquals("12345", match.get("REQUEST_TIME_STAMP"));

    match = huge.match("/name/asc/4/2A3B4C5E6D/2010-10-15T16:51:33Z/foo/"
        + "560261e05f7c21533a3d7d09efff4b91eb7ca702f490e073e1d51614c2e33c1e"
        + "?contenttype=jsonp&jsonp=_jqjsp&_1287161495587=");
    assertNotNull(match);
    assertEquals("2010-10-15T16:51:33Z", match.get("REQUEST_TIME_STAMP"));

    match = huge.match("/name/asc/4/2A3B4C5E6D/2010-10-15T16%253A51%253A33Z/foo/"
        + "560261e05f7c21533a3d7d09efff4b91eb7ca702f490e073e1d51614c2e33c1e"
        + "?contenttype=jsonp%26jsonp=_jqjsp%26_1287161495587");
    assertNotNull(match);
    assertEquals("2010-10-15T16%253A51%253A33Z", match.get("REQUEST_TIME_STAMP"));
  }

  @Test
  public void shouldParseUrlCharactersInParameters() {
    UrlMatch match = pattern.match("/xxx/$-_+*()~/yyy/:,!'%");
    assertNotNull(match);
    assertEquals("$-_+*()~", match.get("a_id"));
    assertEquals(":,!'%", match.get("b_id"));
  }

  @Test
  public void shouldReturnBaseUriAsNormalizedUrlPattern() {
    assertEquals("/xxx/{a_id}/yyy/{b_id}",
        new UrlPattern("/xxx/{a_id}/yyy/{b_id}").getPattern());
    assertEquals("/xxx/{a_id}/yyy/{b_id}",
        new UrlPattern("/xxx/{a_id}/yyy/{b_id}.{format}").getPattern());
  }
}

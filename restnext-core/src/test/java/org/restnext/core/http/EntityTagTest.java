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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Created by thiago on 10/24/16.
 */
public class EntityTagTest {

  @Test
  public void toStringWeakTest() {
    assertEquals(new EntityTag(
            "Hello \"World\"", true).toString(),
        "W/\"Hello \\\"World\\\"\"");
  }

  @Test
  public void toStringStrongTest() {
    assertEquals(new EntityTag(
            "Hello \"World\"").toString(),
        "\"Hello \\\"World\\\"\"");
  }

  @Test
  public void fromStringWeakTest() throws Exception {
    assertEquals(EntityTag.valueOf(
        "W/\"Hello \\\"World\\\"\""),
        new EntityTag("Hello \"World\"", true));
  }

  @Test
  public void fromStringStrongTest() throws Exception {
    assertEquals(EntityTag.valueOf(
        "\"Hello \\\"World\\\"\""),
        new EntityTag("Hello \"World\""));
  }

  @Test
  public void anyMatchTest() throws Exception {
    assertEquals(new EntityTag("*").toString(), "\"*\"");
    assertEquals(new EntityTag("*"), EntityTag.ANY_MATCH);
    assertEquals(new EntityTag("*"), EntityTag.valueOf("*"));
    assertThat(EntityTag.ANY_MATCH, is(new EntityTag("*")));
  }

  @Test
  public void badEntityTagTest() {
    String header = "1\"";
    try {
      EntityTag.valueOf(header);
      fail("RuntimeException expected");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), containsString(header));
    }
    try {
      EntityTag.valueOf(null);
      fail("RuntimeException expected");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is(equalTo("value")));
    }
  }

  @Test
  public void equalsTest() {
    String sw = "lala\"lala";
    String se = "eeee\"eeee";
    EntityTag w = new EntityTag(sw, true);
    EntityTag w2 = new EntityTag(se, true);
    EntityTag e = new EntityTag(sw);
    EntityTag e2 = new EntityTag(se);
    assertNotEquals(w, w2);
    assertNotEquals(e, e2);
    assertNotEquals(w, e2);
    assertNotEquals(e, w2);
    assertNotEquals(w, e);
    assertEquals(e, e);
    assertEquals(e2, e2);
    assertEquals(w, w);
    assertEquals(w2, w2);
  }
}

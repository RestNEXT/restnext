/*
 * Copyright 2017 The Netty Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class SystemPropertyUtilsTest {

  @Before
  public void clearSystemPropertyBeforeEach() {
    System.clearProperty("key");
  }

  @Test(expected = NullPointerException.class)
  public void testGetWithKeyNull() {
    SystemPropertyUtils.get(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithKeyEmpty() {
    SystemPropertyUtils.get("", null);
  }

  @Test
  public void testGetDefaultValueWithPropertyNull() {
    assertEquals("default", SystemPropertyUtils.get("key", "default"));
  }

  @Test
  public void testGetPropertyValue() {
    System.setProperty("key", "value");
    assertEquals("value", SystemPropertyUtils.get("key"));
  }

  @Test
  public void testGetBooleanDefaultValueWithPropertyNull() {
    assertTrue(SystemPropertyUtils.getBoolean("key", true));
    assertFalse(SystemPropertyUtils.getBoolean("key", false));
  }

  @Test
  public void testGetBooleanDefaultValueWithEmptyString() {
    System.setProperty("key", "");
    assertTrue(SystemPropertyUtils.getBoolean("key", true));
    assertFalse(SystemPropertyUtils.getBoolean("key", false));
  }

  @Test
  public void testGetBooleanWithTrueValue() {
    System.setProperty("key", "true");
    assertTrue(SystemPropertyUtils.getBoolean("key", false));
    System.setProperty("key", "yes");
    assertTrue(SystemPropertyUtils.getBoolean("key", false));
    System.setProperty("key", "1");
    assertTrue(SystemPropertyUtils.getBoolean("key", true));
  }

  @Test
  public void testGetBooleanWithFalseValue() {
    System.setProperty("key", "false");
    assertFalse(SystemPropertyUtils.getBoolean("key", true));
    System.setProperty("key", "no");
    assertFalse(SystemPropertyUtils.getBoolean("key", false));
    System.setProperty("key", "0");
    assertFalse(SystemPropertyUtils.getBoolean("key", true));
  }

  @Test
  public void testGetBooleanDefaultValueWithWrongValue() {
    System.setProperty("key", "abc");
    assertTrue(SystemPropertyUtils.getBoolean("key", true));
    System.setProperty("key", "123");
    assertFalse(SystemPropertyUtils.getBoolean("key", false));
  }

  @Test
  public void getIntDefaultValueWithPropertyNull() {
    assertEquals(1, SystemPropertyUtils.getInt("key", 1));
  }

  @Test
  public void getIntWithPropertValueIsInt() {
    System.setProperty("key", "123");
    assertEquals(123, SystemPropertyUtils.getInt("key", 1));
  }

  @Test
  public void getIntDefaultValueWithPropertValueIsNotInt() {
    System.setProperty("key", "NotInt");
    assertEquals(1, SystemPropertyUtils.getInt("key", 1));
  }

  @Test
  public void getLongDefaultValueWithPropertyNull() {
    assertEquals(1, SystemPropertyUtils.getLong("key", 1));
  }

  @Test
  public void getLongWithPropertValueIsLong() {
    System.setProperty("key", "123");
    assertEquals(123, SystemPropertyUtils.getLong("key", 1));
  }

  @Test
  public void getLongDefaultValueWithPropertValueIsNotLong() {
    System.setProperty("key", "NotInt");
    assertEquals(1, SystemPropertyUtils.getLong("key", 1));
  }

}

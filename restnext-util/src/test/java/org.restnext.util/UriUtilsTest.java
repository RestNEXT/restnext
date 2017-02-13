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

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by thiago on 23/11/16.
 */
public class UriUtilsTest {

    @Test
    public void illegalInstatiationUtillityClass() throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        try {
            Constructor c = UriUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(c.getModifiers()));
            c.setAccessible(true);
            c.newInstance();
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(instanceOf(AssertionError.class)));
        }
    }

    @Test
    public void normalizeTest() {
        assertNull(UriUtils.normalize(null));
        assertEquals("/uri", UriUtils.normalize("uri"));
        assertEquals("/uri", UriUtils.normalize("/uri"));
        assertEquals("/uri", UriUtils.normalize("/uri/"));

        assertEquals("/uri/1", UriUtils.normalize("uri/1"));
        assertEquals("/uri/1", UriUtils.normalize("/uri/1"));
        assertEquals("/uri/1", UriUtils.normalize("/uri/1/"));

        assertEquals("/uri/1/2", UriUtils.normalize("uri/1/2"));
        assertEquals("/uri/1/2", UriUtils.normalize("/uri/1/2"));
        assertEquals("/uri/1/2", UriUtils.normalize("/uri/1/2/"));

        assertEquals("/uri/1/2/{name}", UriUtils.normalize("uri/1/2/{name}"));
        assertEquals("/uri/1/2/{name}", UriUtils.normalize("/uri/1/2/{name}"));
        assertEquals("/uri/1/2/{name}", UriUtils.normalize("/uri/1/2/{name}/"));

        assertEquals("/uri/1/2/{name}/\\d+", UriUtils.normalize("uri/1/2/{name}/\\d+"));
        assertEquals("/uri/1/2/{name}/\\d+", UriUtils.normalize("/uri/1/2/{name}/\\d+"));
        assertEquals("/uri/1/2/{name}/\\d+", UriUtils.normalize("/uri/1/2/{name}/\\d+/"));
    }

    @Test
    public void isPathParamUriTest() {
        assertFalse(UriUtils.isPathParamUri(null));

        assertTrue(UriUtils.isPathParamUri("/uri"));
        assertTrue( UriUtils.isPathParamUri("/uri/"));
        assertTrue( UriUtils.isPathParamUri("/uri?"));

        assertTrue(UriUtils.isPathParamUri("/uri/1"));
        assertTrue(UriUtils.isPathParamUri("/uri/1/"));
        assertTrue(UriUtils.isPathParamUri("/uri/1?"));

        assertTrue(UriUtils.isPathParamUri("/uri/1/2"));
        assertTrue(UriUtils.isPathParamUri("/uri/1/2/"));
        assertTrue(UriUtils.isPathParamUri("/uri/1/2?"));

        assertTrue(UriUtils.isPathParamUri("/uri/1/2/{name}"));
        assertTrue(UriUtils.isPathParamUri("/uri/1/2/{name}/"));
        assertTrue(UriUtils.isPathParamUri("/uri/1/2/{name}?"));

        assertFalse(UriUtils.isPathParamUri("/uri/1/2/{name}/\\d+"));
        assertFalse(UriUtils.isPathParamUri("/uri/1/2/{name}/\\d+/"));
        assertFalse(UriUtils.isPathParamUri("/uri/1/2/{name}/\\d+?"));
        assertFalse(UriUtils.isPathParamUri("/uri/1/2/!@#$%*()_-+=[]:;.,|/\\d+?"));
    }

    @Test
    public void pathParamUriConstantTest() {
        assertEquals("^([/])(([/\\w])+(/\\{[\\w]+\\})*)*([?])?$", UriUtils.PATH_PARAM_URI.pattern());
    }

    @Test
    public void addFirstSlashTest() {
        assertNull(UriUtils.addFirstSlash(null));
        assertEquals("/uri", UriUtils.addFirstSlash("uri"));
        assertEquals("/uri", UriUtils.addFirstSlash("/uri"));
    }

    @Test
    public void removeLastSlashTest() {
        assertNull(UriUtils.removeLastSlash(null));
        assertEquals("/uri", UriUtils.removeLastSlash("/uri/"));
        assertEquals("/uri", UriUtils.removeLastSlash("/uri"));
    }

}

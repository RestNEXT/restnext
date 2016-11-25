package org.restnext.util;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by thiago on 23/11/16.
 */
public class ExceptionUtilsTest {

    @Test
    public void illegalInstatiationUtillityClass() throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        try {
            Constructor c = ExceptionUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(c.getModifiers()));
            c.setAccessible(true);
            c.newInstance();
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(instanceOf(AssertionError.class)));
        }
    }

    @Test
    public void getStackTraceAsStringTest() {
        try {
            String a = null;
            a.toString();
        } catch (Exception e) {
            assertThat(ExceptionUtils.getStackTraceAsString(e), startsWith("java.lang.NullPointerException\n" +
                    "\tat org.restnext.util.ExceptionUtilsTest.getStackTraceAsStringTest(ExceptionUtilsTest.java:"));
        }
    }

}

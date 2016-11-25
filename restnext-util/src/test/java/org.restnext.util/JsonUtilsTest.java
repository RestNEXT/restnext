package org.restnext.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by thiago on 23/11/16.
 */
public class JsonUtilsTest {

    private static String json;
    private static Metadata metadata;

    @BeforeClass
    public static void setUp() {
        json = "{\"name\":\"thiago\",\"age\":28}";
        metadata = new Metadata("thiago", 28);
    }

    @Test
    public void illegalInstatiationUtillityClass() throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        try {
            Constructor c = JsonUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(c.getModifiers()));
            c.setAccessible(true);
            c.newInstance();
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(instanceOf(AssertionError.class)));
        }
    }

    @Test
    public void fromJsonTest() throws IOException {
        assertEquals(metadata, JsonUtils.fromJson(json, Metadata.class));
        try (ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes())) {
            assertEquals(metadata, JsonUtils.fromJson(is, Metadata.class));
        }
    }

    @Test
    public void fromInvalidJsonTest() throws IOException {
        String invalidJson = json.replaceAll(":", "-");
        try {
            JsonUtils.fromJson(invalidJson, Metadata.class);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(instanceOf(IOException.class)));
        }

        try (ByteArrayInputStream invalidJsonStream = new ByteArrayInputStream(invalidJson.getBytes())) {
            metadata = JsonUtils.fromJson(invalidJsonStream , Metadata.class);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(instanceOf(IOException.class)));
        }
    }

    @Test
    public void toJsonTest() {
        assertEquals(json, JsonUtils.toJson(metadata));
    }

    private static final class Metadata {
        public String name;
        public int age;
        public Metadata() { super(); }
        public Metadata(String name, int age) {
            this.name = name;
            this.age = age;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Metadata metadata = (Metadata) o;
            return age == metadata.age &&
                    Objects.equals(name, metadata.name);
        }
        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }
}

package org.restnext.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by thiago on 25/11/16.
 */
public class FileUtilsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void illegalInstatiationUtillityClass() throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        try {
            Constructor c = FileUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(c.getModifiers()));
            c.setAccessible(true);
            c.newInstance();
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(instanceOf(AssertionError.class)));
        }
    }

    @Test
    public void removeExtensionTest() throws IOException {
        Path file = folder.newFile("file.txt").toPath();
        Path hiddenFile = folder.newFile(".hiddenFile.txt").toPath();

        assertTrue(Files.exists(file));
        assertTrue(Files.exists(hiddenFile));
        assertEquals("file", FileUtils.removeExtension(file.getFileName()).getFileName().toString());
        assertEquals(".hiddenFile.txt", FileUtils.removeExtension(hiddenFile.getFileName()).getFileName().toString());
    }

    @Test
    public void listChildrenTest() throws IOException {
        Path file = folder.newFile("file.txt").toPath();
        Path file2 = folder.newFile("file2.txt").toPath();
        Path subfolder = folder.newFolder("subfolder").toPath();
        Path subfolder2 = folder.newFolder("subfolder2").toPath();

        Set<Path> children = FileUtils.listChildren(folder.getRoot().toPath(), null);
        Set<Path> expectedChildren = new HashSet<>(Arrays.asList(file, file2, subfolder, subfolder2));
        assertEquals(expectedChildren, children);
        assertEquals(expectedChildren.size(), children.size());

        Set<Path> children2 = FileUtils.listChildren(file, null);
        assertEquals(Collections.emptySet(), children2);
        assertEquals(0, children2.size());

        Set<Path> expectedChildren3 = new HashSet<>(Arrays.asList(file, file2));
        Set<Path> children3 = FileUtils.listChildren(folder.getRoot().toPath(), "*.txt");
        assertEquals(expectedChildren3, children3);
        assertEquals(expectedChildren3.size(), children3.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void listChildrenUnmodifiableTest() throws IOException {
        Path file = folder.newFile("file.txt").toPath();
        Path file2 = folder.newFile("file2.txt").toPath();
        Path subfolder = folder.newFolder("subfolder").toPath();
        Path subfolder2 = folder.newFolder("subfolder2").toPath();

        Set<Path> children = FileUtils.listChildren(folder.getRoot().toPath(), null);
        children.remove(file);
    }
}

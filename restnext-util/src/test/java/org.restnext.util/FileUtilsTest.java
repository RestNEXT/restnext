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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Created by thiago on 25/11/16.
 */
public class FileUtilsTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void illegalInstatiationUtillityClass() throws IllegalAccessException,
      InstantiationException, NoSuchMethodException {
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
    assertEquals("file", FileUtils.removeExtension(file.getFileName()).getFileName()
        .toString());
    assertEquals(".hiddenFile.txt", FileUtils.removeExtension(hiddenFile.getFileName())
        .getFileName().toString());
  }

  @Test
  public void listChildrenTest() throws IOException {
    Path file = folder.newFile("file.txt").toPath();
    Path file2 = folder.newFile("file2.txt").toPath();
    Path subfolder = folder.newFolder("subfolder").toPath();
    Path subfolder2 = folder.newFolder("subfolder2").toPath();

    Set<Path> children = FileUtils.listChildren(folder.getRoot().toPath());
    Set<Path> expectedChildren = new HashSet<>(Arrays.asList(file, file2, subfolder, subfolder2));
    assertEquals(expectedChildren, children);
    assertEquals(expectedChildren.size(), children.size());

    Set<Path> children2 = FileUtils.listChildren(file);
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
    Set<Path> children = FileUtils.listChildren(folder.getRoot().toPath());
    assertEquals(1, children.size());
    children.remove(file);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void listChildrenGlobUnmodifiableTest() throws IOException {
    Path file = folder.newFile("file.txt").toPath();
    Set<Path> children = FileUtils.listChildren(folder.getRoot().toPath(), "*.txt");
    assertEquals(1, children.size());
    children.remove(file);
  }

  @Test
  public void deepListChildrenTest() throws IOException {
    Path file = folder.newFile("file.txt").toPath();
    Path file2 = folder.newFile("file2.txt").toPath();
    Path subfolder = folder.newFolder("subfolder").toPath();
    Path subfolder2 = folder.newFolder("subfolder2").toPath();
    Path subFile = Files.createFile(Paths.get(subfolder.toString(), "subFile.txt"));
    Path subFile2 = Files.createFile(Paths.get(subfolder2.toString(), "subFile2.txt"));

    Set<Path> children = FileUtils.deepListChildren(folder.getRoot().toPath());
    Set<Path> expectedChildren = new HashSet<>(Arrays.asList(file, file2, subfolder, subfolder2,
        subFile, subFile2));
    assertEquals(expectedChildren, children);
    assertEquals(expectedChildren.size(), children.size());

    Set<Path> children2 = FileUtils.deepListChildren(file);
    assertEquals(Collections.emptySet(), children2);
    assertEquals(0, children2.size());

    Set<Path> expectedChildren3 = new HashSet<>(Arrays.asList(file, file2, subFile, subFile2));
    Set<Path> children3 = FileUtils.deepListChildren(folder.getRoot().toPath(), "*.txt");
    assertEquals(expectedChildren3, children3);
    assertEquals(expectedChildren3.size(), children3.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void deepListChildrenUnmodifiableTest() throws IOException {
    Path file = folder.newFile("file.txt").toPath();
    Set<Path> children = FileUtils.deepListChildren(folder.getRoot().toPath());
    assertEquals(1, children.size());
    children.remove(file);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void deepListChildrenGlobUnmodifiableTest() throws IOException {
    Path file = folder.newFile("file.txt").toPath();
    Set<Path> children = FileUtils.deepListChildren(folder.getRoot().toPath(), "*.txt");
    assertEquals(1, children.size());
    children.remove(file);
  }
}

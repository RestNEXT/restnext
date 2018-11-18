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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by thiago on 10/11/16.
 */
public final class FileUtils {

  public static final String DEFAULT_GLOB = "*";

  private FileUtils() {
    throw new AssertionError();
  }

  public static Set<Path> listChildren(Path directory) {
    return listChildren(directory, DEFAULT_GLOB);
  }

  /**
   * List children path from the provided directory and the glob filter.
   *
   * @param directory the directory
   * @param glob      the glob filter
   * @return a set of found path children
   */
  public static Set<Path> listChildren(Path directory, String glob) {
    if (directory == null || !Files.isDirectory(directory)) {
      return Collections.emptySet();
    }
    glob = glob == null || glob.trim().isEmpty() ? DEFAULT_GLOB : glob;
    Set<Path> children = new HashSet<>();
    try (DirectoryStream<Path> childrenStream = Files.newDirectoryStream(directory, glob)) {
      childrenStream.forEach(children::add);
      return Collections.unmodifiableSet(new HashSet<>(children));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Set<Path> deepListChildren(Path directory) {
    return deepListChildren(directory, DEFAULT_GLOB);
  }

  /**
   * Deep list children path from the provided directory and the glob filter.
   *
   * @param directory the directory
   * @param glob      the glob filter
   * @return a set of found path children
   */
  public static Set<Path> deepListChildren(final Path directory, String glob) {
    if (directory == null || !Files.isDirectory(directory)) {
      return Collections.emptySet();
    }
    glob = glob == null || glob.trim().isEmpty() ? DEFAULT_GLOB : glob;
    final PathMatcher pathMatcher = directory.getFileSystem()
        .getPathMatcher("glob:" + glob);
    BiPredicate<Path, BasicFileAttributes> filter = DEFAULT_GLOB.equals(glob.trim())
        ? (path, basicFileAttributes) -> {
          try {
            return !Files.isSameFile(directory, path);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        : (path, basicFileAttributes) -> /*basicFileAttributes.isRegularFile() &&*/
        pathMatcher.matches(path.getFileName());
    try {
      Set<Path> children = Files.find(directory, Integer.MAX_VALUE, filter)
          .collect(Collectors.toSet());
      return Collections.unmodifiableSet(new HashSet<>(children));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Remove the extension information from the path.
   *
   * @param file the path
   * @return the path without extension
   */
  public static Path removeExtension(Path file) {
    Predicate<Path> filter = path -> {
      try {
        return !Files.isDirectory(path) && !Files.isHidden(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };

    Function<Path, Path> removeExtensionFunction = path -> {
      String strPath = path.toString();
      return Paths.get(strPath.substring(0, strPath.lastIndexOf('.')));
    };

    return Optional.ofNullable(file).filter(filter).map(removeExtensionFunction).orElse(file);
  }
}

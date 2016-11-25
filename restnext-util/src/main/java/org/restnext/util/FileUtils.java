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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by thiago on 10/11/16.
 */
public final class FileUtils {

    private FileUtils() {
        throw new AssertionError();
    }

    public static Set<Path> listChildren(final Path directory, final String glob) {
        if (directory == null || !Files.isDirectory(directory)) return Collections.emptySet();
        final String _glob = glob == null || glob.trim().isEmpty() ? "*" : glob;
        final Set<Path> children = new HashSet<>();
        try (DirectoryStream<Path> childrenStream = Files.newDirectoryStream(directory, _glob)) {
            childrenStream.forEach(children::add);
        } catch (IOException ignore) {}
        return Collections.unmodifiableSet(new HashSet<>(children));
    }

    public static Path removeExtension(final Path path) {
        return Optional.ofNullable(path).map(Path::toString)
                .filter(strPath -> strPath.indexOf('.') > 0) // to avoid hidden files.
                .map(strPath -> Paths.get(strPath.substring(0, strPath.lastIndexOf('.'))))
                .orElse(path);
    }

}

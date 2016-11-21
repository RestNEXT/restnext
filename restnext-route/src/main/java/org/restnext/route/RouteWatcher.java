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
package org.restnext.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

/**
 * Created by thiago on 10/13/16.
 */
final class RouteWatcher implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RouteWatcher.class);

    private final RouteScanner routeScanner;

    RouteWatcher(final RouteScanner routeScanner) {
        Objects.requireNonNull(routeScanner, "Route scanner must not be null");
        this.routeScanner = routeScanner;
    }

    @Override
    public void run() {
        watch();
    }

    private void watch() {
        try (FileSystem fs = FileSystems.getDefault()) {
            try (WatchService ws = fs.newWatchService()) {
                Path routeDirectory = routeScanner.getRouteDirectory();
                routeDirectory.register(ws,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);

                PathMatcher jarPathMatcher = fs.getPathMatcher("glob:*.jar");

                LOG.info("Watching route directory for changes - {}", routeDirectory);

                while (true) {
                    WatchKey key = ws.take();
                    for (WatchEvent<?> we : key.pollEvents()) {
                        WatchEvent.Kind<?> wek = we.kind();
                        Path parent = (Path) key.watchable();
                        Path filename = (Path) we.context();

                        if (StandardWatchEventKinds.OVERFLOW.equals(wek)) continue; // ignore event.

                        final Path file = parent.resolve(filename);
                        final boolean isJarFile = jarPathMatcher.matches(filename);

                        if (isJarFile) {
                            if (StandardWatchEventKinds.ENTRY_CREATE.equals(wek) || StandardWatchEventKinds.ENTRY_MODIFY.equals(wek)) {
                                routeScanner.scan(file);
                            } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(wek)) {
                                routeScanner.remove(file);
                            }
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("Could not watch route directory for changes", e);
        }
    }
}

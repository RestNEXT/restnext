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

package org.restnext.security;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thiago on 10/11/16.
 */
final class SecurityWatcher implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityWatcher.class);

  private final SecurityScanner securityScanner;

  SecurityWatcher(final SecurityScanner securityScanner) {
    this.securityScanner = Objects.requireNonNull(securityScanner, "securityScanner");
  }

  @Override
  public void run() {
    watch();
  }

  private void watch() {
    try (FileSystem fs = FileSystems.getDefault()) {
      try (WatchService ws = fs.newWatchService()) {
        Path securityDirectory = securityScanner.getSecurityDirectory();
        securityDirectory.register(ws,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);

        PathMatcher jarPathMatcher = fs.getPathMatcher("glob:*.jar");

        LOG.info("Watching security directory for changes - {}", securityDirectory);

        while (true) {
          WatchKey key = ws.take();
          for (WatchEvent<?> we : key.pollEvents()) {
            WatchEvent.Kind<?> wek = we.kind();
            Path parent = (Path) key.watchable();
            Path filename = (Path) we.context();

            // ignore event.
            if (StandardWatchEventKinds.OVERFLOW.equals(wek)) {
              continue;
            }

            final Path file = parent.resolve(filename);
            final boolean isJarFile = jarPathMatcher.matches(filename);

            if (isJarFile) {
              if (StandardWatchEventKinds.ENTRY_CREATE.equals(wek)
                  || StandardWatchEventKinds.ENTRY_MODIFY.equals(wek)) {
                securityScanner.scan(file);
              } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(wek)) {
                securityScanner.remove(file);
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
      LOG.error("Could not watch security directory for changes", e);
    }
  }

}

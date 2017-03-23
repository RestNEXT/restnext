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

import static org.restnext.util.FileUtils.deepListChildren;
import static org.restnext.util.FileUtils.listChildren;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import javax.xml.bind.JAXBException;

import org.restnext.core.classpath.ClasspathRegister;
import org.restnext.core.http.Request;
import org.restnext.core.jaxb.Jaxb;
import org.restnext.security.jaxb.Securities;
import org.restnext.util.SysPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.joegreen.lambdaFromString.LambdaCreationException;
import pl.joegreen.lambdaFromString.LambdaFactory;
import pl.joegreen.lambdaFromString.LambdaFactoryConfiguration;
import pl.joegreen.lambdaFromString.TypeReference;

/**
 * Created by thiago on 10/11/16.
 */
public final class SecurityScanner {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityScanner.class);

  public static final Path DEFAULT_SECURITY_DIR = SysPropertyUtils.getPath("user.dir", "security");

  private final Security security;
  private final Jaxb securityJaxb;
  private final Path securityDirectory;
  private final Map<Path, Map<Path, Set<Security.Mapping>>> securityJarFilesMap = new HashMap<>();

  private LambdaFactory lambdaFactory;

  // constructors

  public SecurityScanner(final Security security) {
    this(security, DEFAULT_SECURITY_DIR);
  }

  /**
   * Constructor with security and security directory path.
   *
   * @param security          the security
   * @param securityDirectory the security directory path
   */
  public SecurityScanner(final Security security, final Path securityDirectory) {
    Objects.requireNonNull(security, "Security must not be null");
    Objects.requireNonNull(securityDirectory, "Security directory must not be null");

    this.security = security;
    this.securityDirectory = securityDirectory;
    this.securityJaxb = new Jaxb("security.xsd", Securities.class);

    // start task for watching dir for changes.
    new Thread(new SecurityWatcher(this), "security-dir-watcher").start();
  }

  // methods

  private void createLambda(final Set<Path> jars) {
    final String classpath = SysPropertyUtils.get("java.class.path");
    final StringJoiner compilationClassPathJoiner = new StringJoiner(":")
        .add(classpath);

    jars.forEach(jar -> {
      ClasspathRegister.addPath(jar);
      compilationClassPathJoiner.add(jar.toAbsolutePath().toString());
    });

    this.lambdaFactory = LambdaFactory.get(LambdaFactoryConfiguration.get()
        .withCompilationClassPath(compilationClassPathJoiner.toString())
        .withImports(Request.class));

    jars.forEach(this::lookupSecurityFiles);
  }

  public void scan() {
    createLambda(listChildren(securityDirectory, "*.jar"));
  }

  void scan(final Path jar) {
    createLambda(Collections.singleton(jar));
  }

  void remove() {
    listChildren(securityDirectory, "*.jar").forEach(this::remove);
  }

  // getters methods

  void remove(final Path jar) {
    securityJarFilesMap.entrySet().removeIf(jarEntry -> {
      final boolean unregister = jarEntry.getKey().equals(jar.getFileName());
      if (unregister) {
        jarEntry.getValue()
            .forEach(
                (file, securityMappings) -> securityMappings.forEach(this.security::unregister));
      }
      return unregister;
    });
  }

  // private methods

  Path getSecurityDirectory() {
    return securityDirectory;
  }

  private void lookupSecurityFiles(final Path jar) {
    try (FileSystem fs = FileSystems.newFileSystem(jar, null)) {
      final Path securityDirectory = fs.getPath("/META-INF/security/");
      if (Files.exists(securityDirectory)) {
        Set<Path> securityFiles = deepListChildren(securityDirectory, "*.xml");
        securityJarFilesMap.put(jar.getFileName(), readAll(securityFiles));
      }
    } catch (IOException e) {
      LOGGER.error("Could not constructs a new fileSystem to access the contents of the file {} "
          + "as a file system.", jar, e);
    }
  }

  private Map<Path, Set<Security.Mapping>> readAll(final Set<Path> securityFiles) {
    final Map<Path, Set<Security.Mapping>> securityFileMappings =
        new HashMap<>(securityFiles.size());
    securityFiles.forEach(s -> securityFileMappings.put(s, read(s)));
    return Collections.unmodifiableMap(new HashMap<>(securityFileMappings));
  }

  private Set<Security.Mapping> read(final Path securityFile) {
    Set<Security.Mapping> mappings = new HashSet<>();
    try (InputStream is = Files.newInputStream(securityFile)) {
      // deserialize the input stream
      Securities securities = securityJaxb.unmarshal(is, Securities.class);

      // iterates over the entries
      for (Securities.Security security : securities.getSecurity()) {
        String uri = security.getPath();
        Boolean enable = security.getEnable();

        /*
          https://github.com/greenjoe/lambdaFromString#code-examples:
          The compilation process takes time (on my laptop: first call ~1s, subsequent calls ~0.1s)
          so it probably should not be used in places where performance matters.
          The library is rather intended to be used once during the configuration reading process
          when the application starts.
        */
        Function<Request, Boolean> provider = lambdaFactory.createLambda(
            security.getProvider(), new TypeReference<Function<Request, Boolean>>() {
            });

        // checks if already has registered a mapping for the uri.
        // To avoid creating unnecessary mapping objects.
        Security.Mapping mapping = this.security.getSecurityMapping(uri);
        if (mapping == null || !mapping.isEnable()) {
          // builds the mapping
          mapping = Security.Mapping.uri(uri, provider)
              .enable(enable)
              .build();

          // register the mapping
          this.security.register(mapping);
          mappings.add(mapping);
        } else {
          LOGGER.warn("Ignoring the registration of the uri {} of the security file {} in the "
                  + "fileSystem {}, because it was already registered",
              uri, securityFile, securityFile.getFileSystem());
        }
      }
    } catch (IOException | JAXBException | LambdaCreationException e) {
      LOGGER.error("Could not read the security file '{}'", securityFile, e);
    }
    return Collections.unmodifiableSet(new HashSet<>(mappings));
  }
}

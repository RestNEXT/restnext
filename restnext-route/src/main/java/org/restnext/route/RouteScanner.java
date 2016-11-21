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

import org.restnext.core.http.MediaType;
import org.restnext.core.http.codec.Request;
import org.restnext.core.http.codec.Response;
import org.restnext.core.classpath.ClasspathRegister;
import org.restnext.util.JsonUtils;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.joegreen.lambdaFromString.LambdaFactory;
import pl.joegreen.lambdaFromString.LambdaFactoryConfiguration;
import pl.joegreen.lambdaFromString.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

import static org.restnext.util.FileUtils.listChildren;

/**
 * Created by thiago on 10/13/16.
 */
public final class RouteScanner {

    private static final Logger log = LoggerFactory.getLogger(RouteScanner.class);

    public static final Path DEFAULT_ROUTE_DIR = Paths.get(SystemPropertyUtil.get("user.dir"), "route");

    private final Route route;
    private final Path routeDirectory;
    private final Map<Path, Map<Path, Set<String>>> jarRouteFileMap = new HashMap<>(); // the route jars metadata.

    private LambdaFactory lambdaFactory;

    // constructors

    public RouteScanner(final Route route) {
        this(route, DEFAULT_ROUTE_DIR);
    }

    public RouteScanner(final Route route, final Path routeDirectory) {
        Objects.requireNonNull(route, "Route must not be null");
        Objects.requireNonNull(routeDirectory, "Route directory must not be null");

        this.route = route;
        this.routeDirectory = routeDirectory;

        // start task for watching route dir for changes.
        new Thread(new RouteWatcher(this), "route-dir-watcher").start();
    }

    // methods

    public void scan() {
        createLambda(listChildren(routeDirectory, "*.jar"));
    }

    void scan(final Path jar) {
        createLambda(Collections.singleton(jar));
    }

    void remove() {
        listChildren(routeDirectory, "*.jar").forEach(this::remove);
    }

    void remove(final Path jar) {
        Iterator<Map.Entry<Path, Map<Path, Set<String>>>> iterator = jarRouteFileMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Path, Map<Path, Set<String>>> jarEntry = iterator.next();
            Path jarfileNameRegistered = jarEntry.getKey();
            if (jarfileNameRegistered.equals(jar.getFileName())) {
                for (Map.Entry<Path, Set<String>> routeFileEntry : jarEntry.getValue().entrySet()) {
                    routeFileEntry.getValue().forEach(route::unregister);
                    iterator.remove();
                }
            }
        }
    }

    // getters methods

    public Path getRouteDirectory() {
        return routeDirectory;
    }

    // private methods

    private void createLambda(final Set<Path> jars) {
        String classpath = SystemPropertyUtil.get("java.class.path");
        StringJoiner compilationClassPathJoiner = new StringJoiner(":")
                .add(classpath);

        jars.forEach(jar -> {
            ClasspathRegister.addPath(jar);
            compilationClassPathJoiner.add(jar.toAbsolutePath().toString());
        });

        this.lambdaFactory = LambdaFactory.get(LambdaFactoryConfiguration.get()
                .withCompilationClassPath(compilationClassPathJoiner.toString())
                .withImports(Request.class)
                .withImports(Response.class));

        jars.forEach(this::lookupRouteFiles);
    }

    private void lookupRouteFiles(final Path jar) {
        try (FileSystem fs = FileSystems.newFileSystem(jar, null)) {
            final Path internalRouteDir = fs.getPath("/META-INF/route/");
            if (Files.exists(internalRouteDir)) {
                Set<Path> internalRouteFiles = listChildren(internalRouteDir, "*.json");
                Map<Path, Set<String>> routeFileUriMap = new HashMap<>(); // the jars route json metadata.
                internalRouteFiles.forEach(routeFile -> {
                    Path routeFilename = routeFile.getFileName();
                    Set<String> routeUris = routeFileUriMap.get(routeFilename);
                    if (routeUris == null)
                        routeFileUriMap.put(routeFilename, readAndRegister(routeFile));
                    else
                        log.warn("Duplicated route file {} found in the same jar {}", routeFile, jar);
                });
                jarRouteFileMap.put(jar.getFileName(), routeFileUriMap);
            }
        } catch (IOException ignore) {}
    }

    private Set<String> readAndRegister(final Path routeFile) {
        Set<String> registeredUris = new HashSet<>();
        try (InputStream is = Files.newInputStream(routeFile)) {
            final RouteScanner.Metadata[] routesMetadata = JsonUtils.fromJson(is, RouteScanner.Metadata[].class);

            for (RouteScanner.Metadata routeMetadata : routesMetadata) {
                // required metadata.
                final String uri = routeMetadata.getUri();
                final String provider = routeMetadata.getProvider();

                // verify if some required metadata was informed, otherwise ignore.
                // TODO maybe in the future validate it with some json schema implementation.
                if (uri == null && provider == null) continue;

                // optional metadata.
                final boolean enable = routeMetadata.isEnable();
                final List<Request.Method> methods = routeMetadata.getMethods();
                final List<MediaType> medias = routeMetadata.getMedias();

                /*
                  https://github.com/greenjoe/lambdaFromString#code-examples:
                  The compilation process takes time (on my laptop: first call ~1s, subsequent calls ~0.1s)
                  so it probably should not be used in places where performance matters.
                  The library is rather intended to be used once during the configuration reading process when
                  the application starts.
                */
                Function<Request, Response> routeProvider = lambdaFactory.createLambdaUnchecked(provider, new TypeReference<Function<Request,Response>>() {});

                // verify if the uri is already registered by another json file inside some jar file.
                boolean uriAlreadyRegistered = false;
                Path jarFilenameRegistered = null, routeFilenameRegistered = null;
                for (Map.Entry<Path, Map<Path, Set<String>>> jarEntry : jarRouteFileMap.entrySet()) {
                    jarFilenameRegistered = jarEntry.getKey();
                    for (Map.Entry<Path, Set<String>> routeFileEntry : jarEntry.getValue().entrySet()) {
                        routeFilenameRegistered = routeFileEntry.getKey();
                        uriAlreadyRegistered = !routeFilenameRegistered.equals(routeFile.getFileName()) && routeFileEntry.getValue().contains(uri);
                    }
                }

                if (!uriAlreadyRegistered) {
                    Route.Mapping.Builder routeMappingBuilder = Route.Mapping.uri(uri, routeProvider);
                    if (methods != null) routeMappingBuilder.methods(methods.toArray(new Request.Method[methods.size()]));
                    if (medias != null) routeMappingBuilder.medias(medias.toArray(new MediaType[medias.size()]));
                    route.register(routeMappingBuilder.enable(enable).build());
                    registeredUris.add(uri);
                } else {
                    log.warn("Uri {} already registered through the route file {} inside the jar {}", uri, routeFilenameRegistered, jarFilenameRegistered);
                }
            }
        } catch (IOException ignore) {}
        return registeredUris;
    }

    // inner json file metadata class

    private static final class Metadata {

        private String uri;
        private String provider;
        private boolean enable;
        private List<MediaType> medias;
        private List<Request.Method> methods;

        public Metadata() {
            super();
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public List<Request.Method> getMethods() {
            return methods;
        }

        public void setMethods(List<Request.Method> methods) {
            this.methods = methods;
        }

        public List<MediaType> getMedias() {
            return medias;
        }

        public void setMedias(List<MediaType> medias) {
            this.medias = medias;
        }
    }
}

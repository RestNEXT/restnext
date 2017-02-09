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

import io.netty.util.internal.SystemPropertyUtil;
import org.restnext.core.classpath.ClasspathRegister;
import org.restnext.core.http.MediaType;
import org.restnext.core.http.codec.Request;
import org.restnext.core.http.codec.Response;
import org.restnext.core.jaxb.Routes;
import org.restnext.util.JAXB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.joegreen.lambdaFromString.LambdaFactory;
import pl.joegreen.lambdaFromString.LambdaFactoryConfiguration;
import pl.joegreen.lambdaFromString.TypeReference;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.restnext.util.FileUtils.listChildren;

/**
 * Created by thiago on 10/13/16.
 */
public final class RouteScanner {

    private static final Logger log = LoggerFactory.getLogger(RouteScanner.class);

    public static final Path DEFAULT_ROUTE_DIR = Paths.get(SystemPropertyUtil.get("user.dir"), "route");

    private final Route route;
    private final Path routeDirectory;
    private final JAXB routesJaxb;
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
        this.routesJaxb = new JAXB("routes.xsd", Routes.class);

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
                Set<Path> internalRouteFiles = listChildren(internalRouteDir, "*.xml");
                Map<Path, Set<String>> routeFileUriMap = new HashMap<>(); // the jars route metadata.
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
            Routes routes = routesJaxb.unmarshal(is, Routes.class);
            for (Routes.Route route : routes.getRoute()) {
                // required metadata.
                final String uri = route.getPath();
                final String provider = route.getProvider();
                // optional metadata.
                final boolean enable = route.getEnable() == null ? true : route.getEnable();
                final List<String> methodList = route.getMethods().getMethod();
                final List<String> mediaList = route.getMedias().getMedia();
                // parse http methods string to http methods object.
                final List<Request.Method> methods = methodList.stream().map(Request.Method::of).collect(Collectors.toList());
                // parse media types string to media types object.
                final List<MediaType> medias = mediaList.stream().map(MediaType::parse).collect(Collectors.toList());

                /*
                  https://github.com/greenjoe/lambdaFromString#code-examples:
                  The compilation process takes time (on my laptop: first call ~1s, subsequent calls ~0.1s)
                  so it probably should not be used in places where performance matters.
                  The library is rather intended to be used once during the configuration reading process when
                  the application starts.
                */
                Function<Request, Response> routeProvider = lambdaFactory.createLambdaUnchecked(provider, new TypeReference<Function<Request,Response>>() {});

                // verify if the uri is already registered by another jaxb file inside some jar file.
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
                    Route.Mapping.Builder routeMappingBuilder = Route.Mapping.uri(uri, routeProvider).enable(enable);
                    if (methods != null) routeMappingBuilder.methods(methods.toArray(new Request.Method[methods.size()]));
                    if (medias != null) routeMappingBuilder.medias(medias.toArray(new MediaType[medias.size()]));
                    this.route.register(routeMappingBuilder.build());
                    registeredUris.add(uri);
                } else {
                    log.warn("Uri {} already registered through the route file {} inside the jar {}", uri, routeFilenameRegistered, jarFilenameRegistered);
                }
            }
        } catch (IOException | JAXBException e) {
            log.error("Could not register the route metadata in the XML file '{}'.", routeFile, e);
        }
        return registeredUris;
    }
}

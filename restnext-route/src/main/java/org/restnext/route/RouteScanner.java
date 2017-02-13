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
import org.restnext.core.jaxb.Jaxb;
import org.restnext.core.jaxb.internal.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.joegreen.lambdaFromString.LambdaCreationException;
import pl.joegreen.lambdaFromString.LambdaFactory;
import pl.joegreen.lambdaFromString.LambdaFactoryConfiguration;
import pl.joegreen.lambdaFromString.TypeReference;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

import static org.restnext.util.FileUtils.deepListChildren;
import static org.restnext.util.FileUtils.listChildren;

/**
 * Created by thiago on 10/13/16.
 */
public final class RouteScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteScanner.class);

    public static final Path DEFAULT_ROUTE_DIR = Paths.get(SystemPropertyUtil.get("user.dir"), "route");

    private final Route route;
    private final Jaxb routesJaxb;
    private final Path routeDirectory;
    private final Map<Path, Map<Path, Set<Route.Mapping>>> routeJarFilesMap = new HashMap<>();

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
        this.routesJaxb = new Jaxb("routes.xsd", Routes.class);

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
        Iterator<Map.Entry<Path, Map<Path, Set<Route.Mapping>>>> jarIterator = routeJarFilesMap.entrySet().iterator();
        while (jarIterator.hasNext()) {
            Map.Entry<Path, Map<Path, Set<Route.Mapping>>> jarEntry = jarIterator.next();
            if (jarEntry.getKey().equals(jar.getFileName())) {
                for (Map.Entry<Path, Set<Route.Mapping>> routeFileEntry : jarEntry.getValue().entrySet()) {
                    routeFileEntry.getValue().forEach(this.route::unregister);
                    jarIterator.remove();
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
            final Path routeDirectory = fs.getPath("/META-INF/route/");
            if (Files.exists(routeDirectory)) {
                Set<Path> routeFiles = deepListChildren(routeDirectory, "*.xml");
                // maps the jar filename with its security files
                routeJarFilesMap.put(jar.getFileName(), readAll(routeFiles));
            }
        } catch (IOException e) {
            LOGGER.error("Could not constructs a new fileSystem to access the contents of the file {} as a file system.", jar, e);
        }
    }

    private Map<Path, Set<Route.Mapping>> readAll(final Set<Path> routeFiles) {
        final Map<Path, Set<Route.Mapping>> routeFileMappings = new HashMap<>(routeFiles.size());
        // iterates over the security file paths
        for (Path routeFile : routeFiles) {
            // maps the security file path with its security mappings
            routeFileMappings.put(routeFile, read(routeFile));
        }
        return Collections.unmodifiableMap(new HashMap<>(routeFileMappings));
    }

    private Set<Route.Mapping> read(final Path routeFile) {
        Set<Route.Mapping> mappings = new HashSet<>();
        try (InputStream is = Files.newInputStream(routeFile)) {
            // deserialize the input stream
            Routes routes = routesJaxb.unmarshal(is, Routes.class);

            // iterates over the route entries
            for (Routes.Route route : routes.getRoute()) {
                String uri = route.getPath();
                Boolean enable = route.getEnable();
                // parse String http method list to Request.Method array.
                Request.Method[] methods = route.getMethods().getMethod().stream().map(Request.Method::of).toArray(size -> new Request.Method[size]);
                // parse String media type list to MediaType array.
                MediaType[] medias = route.getMedias().getMedia().stream().map(MediaType::parse).toArray(size -> new MediaType[size]);

                /*
                  https://github.com/greenjoe/lambdaFromString#code-examples:
                  The compilation process takes time (on my laptop: first call ~1s, subsequent calls ~0.1s)
                  so it probably should not be used in places where performance matters.
                  The library is rather intended to be used once during the configuration reading process when
                  the application starts.
                */
                Function<Request, Response> provider = lambdaFactory.createLambda(route.getProvider(), new TypeReference<Function<Request, Response>>() {});

                // checks if already has registered a mapping for the uri.
                // To avoid creating unnecessary mapping objects.
                Route.Mapping mapping = this.route.getRouteMapping(uri);
                if (mapping == null || !mapping.isEnable()) {
                    // builds the mapping
                    mapping = Route.Mapping.uri(uri, provider)
                            .enable(enable)
                            .methods(methods)
                            .medias(medias)
                            .build();

                    // register the mapping
                    this.route.register(mapping);
                    mappings.add(mapping);
                } else {
                    LOGGER.warn("Ignoring the registration of the uri {} of the route file {} in the fileSystem {}, because it was already registered",
                            uri, routeFile, routeFile.getFileSystem());
                }
            }
        } catch (IOException | JAXBException | LambdaCreationException e) {
            LOGGER.error("Could not read the route file '{}'", routeFile, e);
        }
        return Collections.unmodifiableSet(new HashSet<>(mappings));
    }
}

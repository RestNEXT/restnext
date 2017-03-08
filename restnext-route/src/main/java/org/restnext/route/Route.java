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

import static org.restnext.util.UriUtils.isPathParamUri;
import static org.restnext.util.UriUtils.normalize;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import org.restnext.core.http.MediaType;
import org.restnext.core.http.codec.Request;
import org.restnext.core.http.codec.Response;
import org.restnext.core.http.url.UrlMatcher;
import org.restnext.core.http.url.UrlPattern;
import org.restnext.core.http.url.UrlRegex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thiago on 10/13/16.
 */
public enum Route {

  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(Route.class);

  private final Map<String, Route.Mapping> registry = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  /**
   * Register a route mapping.
   *
   * @param routeMapping the route mapping to be registered
   */
  public final void register(final Route.Mapping routeMapping) {
    Objects.requireNonNull(routeMapping, "Route mapping must not be null");

    final String uri = routeMapping.getUri();
    final Route.Mapping routeMappingRegistered = registry.get(uri);

    if (routeMappingRegistered == null || !routeMappingRegistered.isEnable()) {
      registry.put(uri, routeMapping);
      LOGGER.debug("The route uri '{}' was registered", uri);
    } else {
      LOGGER.warn("The route uri '{}' is already registered", uri);
    }
  }

  public void unregister(final Route.Mapping mapping) {
    unregister(mapping.getUri());
  }

  /**
   * Unregister a uri.
   *
   * @param uri the uri to be unregistered
   */
  public void unregister(final String uri) {
    Objects.requireNonNull(uri, "Uri must not be null");

    if (getRouteMapping(uri) != null) {
      registry.remove(uri);
      LOGGER.debug("The route uri {} was unregistered", uri);
    }
  }

  /**
   * Get a route mapping from provided uri.
   *
   * @param uri the uri
   * @return the route mapping
   */
  public Route.Mapping getRouteMapping(final String uri) {
    for (Route.Mapping routeMapping : registry.values()) {
      if (routeMapping.getUrlMatcher().matches(uri)) {
        return routeMapping;
      }
    }
    return null;
  }

  // inner mapping class

  public static final class Mapping {

    private final String uri;
    private final boolean enable;
    private final UrlMatcher urlMatcher;
    private final List<MediaType> medias;
    private final List<Request.Method> methods;
    private final Function<Request, Response> routeProvider;

    private Mapping(final Route.Mapping.Builder builder) {
      this.uri = builder.uri;
      this.enable = builder.enable;
      this.medias = builder.medias;
      this.methods = builder.methods;
      this.routeProvider = builder.provider;
      this.urlMatcher = builder.urlMatcher;
    }

    // getters methods

    public static Builder uri(final String uri, final Function<Request, Response> provider) {
      return new Route.Mapping.Builder(uri, provider);
    }

    public String getUri() {
      return uri;
    }

    public boolean isEnable() {
      return enable;
    }

    public UrlMatcher getUrlMatcher() {
      return urlMatcher;
    }

    public List<MediaType> getMedias() {
      return medias;
    }

    public List<Request.Method> getMethods() {
      return methods;
    }

    // convenient methods

    public Response writeResponse(final Request request) {
      return getRouteProvider().apply(request);
    }

    // convenient static methods

    public Function<Request, Response> getRouteProvider() {
      return routeProvider;
    }

    // inner builder class

    public static final class Builder {

      // required params.
      private final String uri;
      private final UrlMatcher urlMatcher;
      private final Function<Request, Response> provider;

      // optional params - initialized to default values.
      private boolean enable = true;
      private List<MediaType> medias = Collections.emptyList();
      private List<Request.Method> methods = Collections.emptyList();

      /**
       * Constructor with uri and provided function.
       *
       * @param uri the uri
       * @param provider the provider function
       */
      public Builder(final String uri, final Function<Request, Response> provider) {
        Objects.requireNonNull(uri, "Uri must not be null");
        Objects.requireNonNull(provider, "Provider must not be null");

        this.uri = normalize(uri);
        this.provider = provider;
        this.urlMatcher = isPathParamUri(this.uri)
            ? new UrlPattern(this.uri)
            : new UrlRegex(this.uri);
      }

      /**
       * Enable the route mapping.
       *
       * @param enable true to enable, otherwise false
       * @return the route mapping builder
       */
      public Builder enable(Boolean enable) {
        // if null fallback to default value to avoid NullPoiterException
        if (enable != null) {
          this.enable = enable;
        }
        return this;
      }

      /**
       * Add route mapping media type support.
       *
       * @param medias the media types
       * @return the route mapping builder
       */
      public Builder medias(MediaType... medias) {
        if (medias != null) {
          this.medias = Arrays.asList(medias);
        }
        return this;
      }

      /**
       * Add route mapping method support.
       *
       * @param methods the http methods
       * @return the route mapping builder
       */
      public Builder methods(Request.Method... methods) {
        if (methods != null) {
          this.methods = Arrays.asList(methods);
        }
        return this;
      }

      public Route.Mapping build() {
        return new Route.Mapping(this);
      }
    }
  }
}

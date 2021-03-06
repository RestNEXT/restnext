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

import static org.restnext.util.UriUtils.isPathParamUri;
import static org.restnext.util.UriUtils.normalize;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import org.restnext.core.http.Request;
import org.restnext.core.url.UrlMatcher;
import org.restnext.core.url.UrlPattern;
import org.restnext.core.url.UrlRegex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thiago on 10/7/16.
 */
public enum Security {

  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(Security.class);

  private final Map<String, Security.Mapping> registry =
      new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  /**
   * Check the request authorization.
   *
   * @param request the request
   * @return true if its ok, otherwise false
   */
  public static boolean checkAuthorization(final Request request) {
    Objects.requireNonNull(request, "request");
    return Optional.ofNullable(Security.INSTANCE.getSecurityMapping(request.getUri().toString()))
        .filter(Security.Mapping::isEnable)
        .map(securityMapping -> securityMapping.getSecurityProvider().apply(request))
        .orElse(true);
  }

  /**
   * Get a security mapping from provided uri.
   *
   * @param uri the uri
   * @return the security mapping
   */
  public Security.Mapping getSecurityMapping(final String uri) {
    for (Security.Mapping securityMapping : registry.values()) {
      if (securityMapping.getUrlMatcher().matches(uri)) {
        return securityMapping;
      }
    }
    return null;
  }

  /**
   * Register a security mapping.
   *
   * @param securityMapping the security mapping
   */
  public final void register(final Security.Mapping securityMapping) {
    Objects.requireNonNull(securityMapping, "securityMapping");

    final String uri = securityMapping.getUri();
    final Security.Mapping securityMappingRegistered = registry.get(uri);

    if (securityMappingRegistered == null || !securityMappingRegistered.isEnable()) {
      registry.put(uri, securityMapping);
      LOGGER.debug("The security uri '{}' was registered.", uri);
    } else {
      LOGGER.warn("The security uri '{}' is already registered.", uri);
    }
  }

  public void unregister(final Security.Mapping mapping) {
    unregister(mapping.getUri());
  }

  // convenient static methods

  /**
   * Unregister a uri.
   *
   * @param uri the uri
   */
  public void unregister(final String uri) {
    if (getSecurityMapping(Objects.requireNonNull(uri, "uri")) != null) {
      registry.remove(uri);
      LOGGER.debug("The security uri {} was unregistered", uri);
    }
  }

  // inner mapping class

  public static final class Mapping {

    private final String uri;
    private final boolean enable;
    private final UrlMatcher urlMatcher;
    private final Function<Request, Boolean> securityProvider;

    private Mapping(final Security.Mapping.Builder builder) {
      this.uri = builder.uri;
      this.enable = builder.enable;
      this.urlMatcher = builder.urlMatcher;
      this.securityProvider = builder.provider;
    }

    // getters methods

    public static Builder uri(final String uri, final Function<Request, Boolean> provider) {
      return new Security.Mapping.Builder(uri, provider);
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

    // convenient static methods

    public Function<Request, Boolean> getSecurityProvider() {
      return securityProvider;
    }

    // inner builder class

    public static final class Builder {

      // required params.
      private final String uri;
      private final UrlMatcher urlMatcher;
      private final Function<Request, Boolean> provider;

      // optional params - initialized to default values.
      private boolean enable = true;

      /**
       * Constructor with uri and provider function.
       *
       * @param uri      the uri
       * @param provider the provider function
       */
      public Builder(final String uri, final Function<Request, Boolean> provider) {
        this.uri = normalize(Objects.requireNonNull(uri, "uri"));
        this.provider = Objects.requireNonNull(provider, "provider");
        this.urlMatcher = isPathParamUri(this.uri)
            ? new UrlPattern(this.uri)
            : new UrlRegex(this.uri);
      }

      /**
       * Enable the security mapping.
       *
       * @param enable true to enable, otherwise false
       * @return the security mapping builder
       */
      public Builder enable(Boolean enable) {
        // if null fallback to default value to avoid NullPoiterException
        if (enable != null) {
          this.enable = enable;
        }
        return this;
      }

      public Security.Mapping build() {
        return new Security.Mapping(this);
      }
    }
  }
}

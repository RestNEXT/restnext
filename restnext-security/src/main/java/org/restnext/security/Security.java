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

import org.restnext.core.http.codec.Request;
import org.restnext.core.http.url.UrlMatcher;
import org.restnext.core.http.url.UrlPattern;
import org.restnext.core.http.url.UrlRegex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import static org.restnext.util.UriUtils.isPathParamUri;
import static org.restnext.util.UriUtils.normalize;

/**
 * Created by thiago on 10/7/16.
 */
public enum Security {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Security.class);

    private final Map<String, Security.Mapping> registry = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public final void register(final Security.Mapping securityMapping) {
        Objects.requireNonNull(securityMapping, "Security mapping must not be null");

        final String uri = securityMapping.getUri();
        final Security.Mapping securityMappingRegistered = registry.get(uri);

        if (securityMappingRegistered == null || !securityMappingRegistered.isEnable()) {
            registry.put(uri, securityMapping);
            LOGGER.debug("the security uri '{}' was registered.", uri);
        } else {
            LOGGER.warn("the security uri '{}' is already registered.", uri);
        }
    }

    public void unregister(final String uri) {
        if (getSecurityMapping(uri) != null) registry.remove(uri);
    }

    public void unregister(final Security.Mapping mapping) {
        unregister(mapping.getUri());
    }

    public Security.Mapping getSecurityMapping(final String uri) {
        for (Security.Mapping securityMapping : registry.values()) {
            if (securityMapping.getUrlMatcher().matches(uri))
                return securityMapping;
        }
        return null;
    }

    // convenient static methods

    public static boolean checkAuthorization(final Request request) {
        Objects.requireNonNull(request, "Request must not be null");
        Objects.requireNonNull(request.getURI(), "Request uri must not be null");

        return Optional.ofNullable(Security.INSTANCE.getSecurityMapping(request.getURI()))
                .filter(Security.Mapping::isEnable)
                .map(securityMapping -> securityMapping.getSecurityProvider().apply(request))
                .orElse(true);
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

        public String getUri() {
            return uri;
        }

        public boolean isEnable() {
            return enable;
        }

        public UrlMatcher getUrlMatcher() {
            return urlMatcher;
        }

        public Function<Request, Boolean> getSecurityProvider() {
            return securityProvider;
        }

        // convenient static methods

        public static Builder uri(final String uri, final Function<Request, Boolean> provider) {
            return new Security.Mapping.Builder(uri, provider);
        }

        // inner builder class

        public static final class Builder {

            // required params.
            private final String uri;
            private final UrlMatcher urlMatcher;
            private final Function<Request, Boolean> provider;

            // optional params - initialized to default values.
            private boolean enable = true;

            public Builder(final String uri, final Function<Request, Boolean> provider) {
                Objects.requireNonNull(uri, "Uri must not be null");
                Objects.requireNonNull(provider, "Provider must not be null");

                this.uri = normalize(uri);
                this.provider = provider;
                this.urlMatcher = isPathParamUri(this.uri) ? new UrlPattern(this.uri) : new UrlRegex(this.uri);
            }

            public Builder enable(Boolean enable) {
                // if null fallback to default value to avoid NullPoiterException
                if (enable != null) this.enable = enable;
                return this;
            }

            public Security.Mapping build() {
                return new Security.Mapping(this);
            }
        }
    }
}

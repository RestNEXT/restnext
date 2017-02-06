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
package org.restnext.core.http.codec;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import org.restnext.core.http.EntityTag;
import org.restnext.core.http.MediaType;
import org.restnext.core.http.MultivaluedHashMap;
import org.restnext.core.http.MultivaluedMap;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * Created by thiago on 24/08/16.
 */
final class ResponseImpl implements Response {

    private final FullHttpResponse response;
    private final Version version;
    private final Status status;
    private final byte[] content;
    private final MultivaluedMap<String, String> headers;

    private ResponseImpl(final Builder builder) {
        this.response = builder.response;
        this.version = builder.version;
        this.status = builder.status;
        this.content = builder.content;

        // copy the outbound netty response headers.
        this.headers = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : response.headers()) {
            this.headers.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public FullHttpResponse getFullHttpResponse() {
        return response;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public boolean hasContent() {
        return getLength() > 0;
    }

    @Override
    public MediaType getMediaType() {
        String header = getHeader(CONTENT_TYPE);
        return header != null ? MediaType.parse(header) : null;
    }

    @Override
    public int getLength() {
        return getContent() == null ? 0 : getContent().length;
    }

    @Override
    public Set<Request.Method> getAllowedMethods() {
        String header = getHeader(ALLOW);
        if (header != null) {
            return Stream.of(header.split(","))
                    .map(Request.Method::of)
                    .collect(Collectors.toSet());
        }
//        List<String> allowedMethods = getAllHeader(ALLOW);
//        if (allowedMethods != null) {
//            return allowedMethods.stream()
//                    .map(Request.Method::of)
//                    .collect(Collectors.toSet());
//        }
        return null;

    }

    @Override
    public EntityTag getEntityTag() {
        String header = getHeader(ETAG);
        return header != null ? EntityTag.fromString(header) : null;
    }

    @Override
    public Date getLastModified() {
        String header = getHeader(LAST_MODIFIED);
        return header != null ? DateFormatter.parseHttpDate(header) : null;
    }

    @Override
    public URI getLocation() {
        String header = getHeader(LOCATION);
        return header != null ? URI.create(header) : null;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    //============================
    //          BUILDER
    //============================

    public static final class Builder implements Response.Builder {

        private FullHttpResponse response;
        private Version version = Version.HTTP_1_1;
        private Status status = Status.OK;
        private byte[] content;

        private final HttpHeaders headers = new DefaultHttpHeaders(true);

        @Override
        public Response build() {
            // Create the FullHttpResponse object.
            this.response = new DefaultFullHttpResponse(
                    this.version.getNettyVersion(),
                    this.status.getNettyStatus(),
                    this.content == null
                            ? Unpooled.EMPTY_BUFFER
                            : Unpooled.copiedBuffer(this.content));

            // Set my mandatory Date header, if necessary.
            if (this.headers.get(DATE) == null) date(new Date());

            // Adds some custom headers.
            this.headers.set(SERVER, "RestNEXT");
            this.headers.set("x-powered-by", "Netty");

            // Set outbound headers in the response object.
            for (String name : this.headers.names()) {
                List<String> values = this.headers.getAll(name);
                for (String value : values) {
                    this.response.headers().add(name, value);
                }
            }

            // Create the response object.
            return new ResponseImpl(this);
        }

        @Override
        public Response.Builder version(Version version) {
            if (version != null) this.version = version;
            return this;
        }

        @Override
        public Response.Builder content(byte[] content) {
            this.content = content;
            return this;
        }

        @Override
        public Response.Builder status(Status status) {
            if (status != null) this.status = status;
            return this;
        }

        @Override
        public Response.Builder status(int status) {
            return status(Status.fromStatusCode(status));
        }

        @Override
        public Response.Builder allow(Set<Request.Method> methods) {
            return setHeader(ALLOW, methods == null ? null : methods.stream().map(Request.Method::name).collect(Collectors.joining(",")));
        }

        @Override
        public Response.Builder allow(Request.Method... methods) {
            return allow(new HashSet<>(Arrays.asList(methods)));
        }

        @Override
        public Response.Builder encoding(String encoding) {
            return setHeader(CONTENT_ENCODING, encoding);
        }

        @Override
        public Response.Builder type(MediaType type) {
            return setHeader(CONTENT_TYPE, type);
        }

        @Override
        public Response.Builder type(String type) {
            return type(type == null ? null : MediaType.parse(type));
        }

        @Override
        public Response.Builder expires(Date expires) {
            return setHeader(EXPIRES, expires == null ? null : DateFormatter.format(expires));
        }

        @Override
        public Response.Builder date(Date date) {
            return setHeader(DATE, date == null ? null : DateFormatter.format(date));
        }

        @Override
        public Response.Builder lastModified(Date lastModified) {
            return setHeader(LAST_MODIFIED, lastModified == null ? null : DateFormatter.format(lastModified));
        }

        @Override
        public Response.Builder tag(EntityTag tag) {
            return setHeader(ETAG, tag);
        }

        @Override
        public Response.Builder tag(String tag) {
            return tag(tag == null ? null : new EntityTag(tag));
        }

        @Override
        public Response.Builder language(String language) {
            return setHeader(CONTENT_LANGUAGE, language);
        }

        @Override
        public Response.Builder language(Locale locale) {
            return setHeader(CONTENT_LANGUAGE, locale);
        }

        @Override
        public Response.Builder location(URI location) {
            return setHeader(LOCATION, location);
        }

        @Override
        public Response.Builder contentLocation(URI location) {
            return setHeader(CONTENT_LOCATION, location);
        }

        @Override
        public Response.Builder setHeader(CharSequence name, Object value) {
            return header(name, value, false);
        }

        @Override
        public Response.Builder addHeader(CharSequence name, Object value) {
            return header(name, value, true);
        }

        public Response.Builder addHeader(AsciiString name, Object value) {
            return header(name.toString(), value, true);
        }

        public Response.Builder setHeader(AsciiString name, Object value) {
            return header(name.toString(), value, false);
        }

        public Response.Builder header(CharSequence name, Object value, boolean combined) {
            if (value != null) {
                if (combined) {
                    this.headers.add(name, value);
                } else {
                    this.headers.set(name, value);
                }
            } else {
                this.headers.remove(name);
            }
            return this;
        }
    }

}

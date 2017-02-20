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

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.restnext.core.http.EntityTag;
import org.restnext.core.http.MediaType;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public interface Response extends Message, Headers {

    FullHttpResponse getFullHttpResponse();

    Status getStatus();

    byte[] getContent();

    boolean hasContent();

    MediaType getMediaType();

    int getLength();

    Set<Request.Method> getAllowedMethods();

    EntityTag getEntityTag();

    Date getLastModified();

    URI getLocation();

    //============================
    //        HTTP STATUS
    //============================

    enum Status {

        OK(HttpResponseStatus.OK),
        CREATED(HttpResponseStatus.CREATED),
        ACCEPTED(HttpResponseStatus.ACCEPTED),
        NO_CONTENT(HttpResponseStatus.NO_CONTENT),
        RESET_CONTENT(HttpResponseStatus.RESET_CONTENT),
        PARTIAL_CONTENT(HttpResponseStatus.PARTIAL_CONTENT),
        MOVED_PERMANENTLY(HttpResponseStatus.MOVED_PERMANENTLY),
        FOUND(HttpResponseStatus.FOUND),
        SEE_OTHER(HttpResponseStatus.SEE_OTHER),
        NOT_MODIFIED(HttpResponseStatus.NOT_MODIFIED),
        USE_PROXY(HttpResponseStatus.USE_PROXY),
        TEMPORARY_REDIRECT(HttpResponseStatus.TEMPORARY_REDIRECT),
        BAD_REQUEST(HttpResponseStatus.BAD_REQUEST),
        UNAUTHORIZED(HttpResponseStatus.UNAUTHORIZED),
        PAYMENT_REQUIRED(HttpResponseStatus.PAYMENT_REQUIRED),
        FORBIDDEN(HttpResponseStatus.FORBIDDEN),
        NOT_FOUND(HttpResponseStatus.NOT_FOUND),
        METHOD_NOT_ALLOWED(HttpResponseStatus.METHOD_NOT_ALLOWED),
        NOT_ACCEPTABLE(HttpResponseStatus.NOT_ACCEPTABLE),
        PROXY_AUTHENTICATION_REQUIRED(HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED),
        REQUEST_TIMEOUT(HttpResponseStatus.REQUEST_TIMEOUT),
        CONFLICT(HttpResponseStatus.CONFLICT),
        GONE(HttpResponseStatus.GONE),
        LENGTH_REQUIRED(HttpResponseStatus.LENGTH_REQUIRED),
        PRECONDITION_FAILED(HttpResponseStatus.PRECONDITION_FAILED),
        REQUEST_ENTITY_TOO_LARGE(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE),
        REQUEST_URI_TOO_LONG(HttpResponseStatus.REQUEST_URI_TOO_LONG),
        UNSUPPORTED_MEDIA_TYPE(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE),
        REQUESTED_RANGE_NOT_SATISFIABLE(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE),
        EXPECTATION_FAILED(HttpResponseStatus.EXPECTATION_FAILED),
        INTERNAL_SERVER_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR),
        NOT_IMPLEMENTED(HttpResponseStatus.NOT_IMPLEMENTED),
        BAD_GATEWAY(HttpResponseStatus.BAD_GATEWAY),
        SERVICE_UNAVAILABLE(HttpResponseStatus.SERVICE_UNAVAILABLE),
        GATEWAY_TIMEOUT(HttpResponseStatus.GATEWAY_TIMEOUT),
        HTTP_VERSION_NOT_SUPPORTED(HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED),
        CONTINUE(HttpResponseStatus.CONTINUE);

        private static class Holder {
            static Map<HttpResponseStatus, Status> MAP = new HashMap<>();
        }

        private final int code;
        private final String reason;
        private final Family family;
        private final HttpResponseStatus nettyStatus;

        Status(final HttpResponseStatus status) {
            Holder.MAP.put(status, this);
            this.nettyStatus = status;
            this.code = status.code();
            this.reason = status.reasonPhrase();
            this.family = Family.familyOf(status.code());
        }

        public Family getFamily() {
            return family;
        }

        public int getStatusCode() {
            return code;
        }

        public String getReasonPhrase() {
            return reason;
        }

        public HttpResponseStatus getNettyStatus() {
            return nettyStatus;
        }

        @Override
        public String toString() {
            return getReasonPhrase();
        }

        public static Status fromStatusCode(int statusCode) {
            return fromStatus(HttpResponseStatus.valueOf(statusCode));
        }

        public static Status fromStatus(HttpResponseStatus status) {
            return Holder.MAP.getOrDefault(status, null);
        }

        public enum Family {

            INFORMATIONAL,
            SUCCESSFUL,
            REDIRECTION,
            CLIENT_ERROR,
            SERVER_ERROR,
            OTHER;

            public static Family familyOf(final int statusCode) {
                switch (statusCode / 100) {
                    case 1: return Family.INFORMATIONAL;
                    case 2: return Family.SUCCESSFUL;
                    case 3: return Family.REDIRECTION;
                    case 4: return Family.CLIENT_ERROR;
                    case 5: return Family.SERVER_ERROR;
                    default: return Family.OTHER;
                }
            }
        }
    }

    //============================
    //          BUILDER
    //============================

    interface Builder {

        Response build();

        Response.Builder version(Version version);

        Response.Builder status(Status status);

        Response.Builder status(int status);

        Response.Builder content(byte[] content);

        Response.Builder content(String content);

        Response.Builder content(String content, Charset charset);

        Response.Builder allow(Request.Method... methods);

        Response.Builder allow(Set<Request.Method> methods);

        Response.Builder encoding(String encoding);

        Response.Builder setHeader(CharSequence name, Object value);

        Response.Builder addHeader(CharSequence name, Object value);

        Response.Builder type(MediaType type);

        Response.Builder type(String type);

        Response.Builder expires(Date expires);

        Response.Builder date(Date date);

        Response.Builder lastModified(Date lastModified);

        Response.Builder tag(EntityTag tag);

        Response.Builder tag(String tag);

        Response.Builder language(String language);

        Response.Builder language(Locale language);

        Response.Builder location(URI location);

        Response.Builder contentLocation(URI location);
    }

    //============================
    //     STATIC METHODS
    //============================

    static Response.Builder fromResponse(FullHttpResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        Response.Builder builder = status(Status.fromStatus(response.status()));
        builder.version(Version.of(response.protocolVersion()));
        builder.content(response.content().array());

        for (String name : response.headers().names()) {
            for (String value : response.headers().getAll(name)) {
                builder.addHeader(name, value);
            }
        }

        return builder;
    }

    static Response.Builder fromResponse(Response response) {
        Objects.requireNonNull(response, "response must not be null");

        Response.Builder builder = status(response.getStatus());
        builder.version(response.getVersion());
        builder.content(response.getContent());

        for (String name : response.getHeaders().keySet()) {
            for (String value : response.getHeaders().get(name)) {
                builder.addHeader(name, value);
            }
        }

        return builder;
    }

    static Response.Builder status(int status) {
        return status(Status.fromStatusCode(status));
    }

    static Response.Builder status(Status status) {
        return new ResponseImpl.Builder().status(status);
    }

    static Response.Builder ok() {
        return status(Status.OK);
    }

    static Response.Builder ok(byte[] content) {
        return ok().content(content);
    }

    static Response.Builder ok(byte[] content, MediaType type) {
        return ok(content).type(type);
    }

    static Response.Builder ok(byte[] content, String type) {
        return ok(content, MediaType.parse(type));
    }

    static Response.Builder ok(MediaType type) {
        return ok().type(type);
    }

    static Response.Builder ok(String content) {
        return ok(content, StandardCharsets.UTF_8);
    }

    static Response.Builder ok(String content, Charset charset) {
        return ok(content, charset, MediaType.TEXT_UTF8);
    }

    static Response.Builder ok(String content, String mediaType) {
        return ok(content, StandardCharsets.UTF_8, MediaType.parse(mediaType));
    }

    static Response.Builder ok(String content, MediaType mediaType) {
        return ok(content, StandardCharsets.UTF_8, mediaType);
    }

    static Response.Builder ok(String content, Charset charset, MediaType mediaType) {
        return ok(content.getBytes(charset), mediaType);
    }

    static Response.Builder serverError() {
        return status(Status.INTERNAL_SERVER_ERROR);
    }

    static Response.Builder created(URI location) {
        return status(Status.CREATED).location(location);
    }

    static Response.Builder accepted() {
        return status(Status.ACCEPTED);
    }

    static Response.Builder noContent() {
        return status(Status.NO_CONTENT);
    }

    static Response.Builder notModified() {
        return status(Status.NOT_MODIFIED);
    }

    static Response.Builder notModified(EntityTag tag) {
        return notModified().tag(tag);
    }

    static Response.Builder notModified(String tag) {
        return notModified().tag(tag);
    }

    static Response.Builder seeOther(URI location) {
        return status(Status.SEE_OTHER).location(location);
    }

    static Response.Builder temporaryRedirect(URI location) {
        return status(Status.TEMPORARY_REDIRECT).location(location);
    }

    static Response.Builder redirect(URI location) {
        return status(Status.FOUND).location(location);
    }

}

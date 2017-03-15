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

package org.restnext.core.http;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public interface Response extends Message, Headers {

  Status getStatus();

  byte[] getContent();

  boolean hasContent();

  MediaType getMediaType();

  int getLength();

  Set<Request.Method> getAllowedMethods();

  EntityTag getEntityTag();

  Date getLastModified();

  URI getLocation();

  enum Status {

    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),
    RESET_CONTENT(205, "Reset Content"),
    PARTIAL_CONTENT(206, "Partial Content"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    USE_PROXY(305, "Use Proxy"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED(402, "Payment Required"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    LENGTH_REQUIRED(411, "Length Required"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
    REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
    EXPECTATION_FAILED(417, "Expectation Failed"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
    CONTINUE(100, "Continue");

    private final int code;
    private final String reason;
    private final Family family;

    Status(int code, String reason) {
      Holder.MAP.put(code, this);
      this.code = code;
      this.reason = reason;
      this.family = Family.familyOf(code);
    }

    public static Status fromStatusCode(int statusCode) {
      return Holder.MAP.getOrDefault(statusCode, null);
    }

    public Family getFamily() {
      return family;
    }

    public int getStatusCode() {
      return code;
    }

    @Override
    public String toString() {
      return getReasonPhrase();
    }

    public String getReasonPhrase() {
      return reason;
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
          case 1:
            return Family.INFORMATIONAL;
          case 2:
            return Family.SUCCESSFUL;
          case 3:
            return Family.REDIRECTION;
          case 4:
            return Family.CLIENT_ERROR;
          case 5:
            return Family.SERVER_ERROR;
          default:
            return Family.OTHER;
        }
      }
    }

    private static class Holder {
      static Map<Integer, Status> MAP = new HashMap<>();
    }
  }

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

    Response.Builder language(Locale locale);

    Response.Builder location(URI location);

    Response.Builder contentLocation(URI location);

  }

  // convenient methods

  static Response.Builder status(Status status) {
    return new ResponseImpl.Builder().status(status);
  }

  static Response.Builder status(int status) {
    return status(Status.fromStatusCode(status));
  }

  static Response.Builder ok(byte[] content, String type) {
    return ok(content, MediaType.parse(type));
  }

  static Response.Builder ok(byte[] content, MediaType type) {
    return ok(content).type(type);
  }

  static Response.Builder ok(byte[] content) {
    return ok().content(content);
  }

  static Response.Builder ok() {
    return status(Status.OK);
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

  static Response.Builder ok(String content, Charset charset, MediaType mediaType) {
    return ok(content.getBytes(charset), mediaType);
  }

  static Response.Builder ok(String content, String mediaType) {
    return ok(content, StandardCharsets.UTF_8, MediaType.parse(mediaType));
  }

  static Response.Builder ok(String content, MediaType mediaType) {
    return ok(content, StandardCharsets.UTF_8, mediaType);
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

  static Response.Builder notModified(EntityTag tag) {
    return notModified().tag(tag);
  }

  static Response.Builder notModified() {
    return status(Status.NOT_MODIFIED);
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

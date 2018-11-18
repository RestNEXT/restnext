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

import static io.netty.handler.codec.DateFormatter.parseHttpDate;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.DATE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_MATCH;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_NONE_MATCH;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_UNMODIFIED_SINCE;
import static org.restnext.util.UriUtils.normalize;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.AsciiString;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thiago on 24/08/16.
 */
public final class RequestImpl implements Request {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestImpl.class);

  private final Version version;
  private final Method method;
  private final URI baseUri;
  private final URI uri;
  private final boolean keepAlive;
  private final MultivaluedMap<String, String> headers;
  private final MultivaluedMap<String, String> parameters;
  private final Charset charset;
  private byte[] content;

  /**
   * Create a new instance.
   *
   * @param context netty channel handler context
   * @param request netty full http request
   */
  public RequestImpl(final ChannelHandlerContext context, final FullHttpRequest request) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(context, "context");

    this.charset = HttpUtil.getCharset(request, StandardCharsets.UTF_8);
    this.version = HttpVersion.HTTP_1_0.equals(request.protocolVersion())
        ? Version.HTTP_1_0
        : Version.HTTP_1_1;
    this.method = Method.valueOf(request.method().name());
    this.uri = URI.create(normalize(request.uri()));
    this.baseUri = createBaseUri(context, request);
    this.keepAlive = HttpUtil.isKeepAlive(request);

    // copy the inbound netty request headers.
    this.headers = new MultivaluedHashMap<>();
    for (Map.Entry<String, String> entry : request.headers()) {
      this.headers.add(entry.getKey().toLowerCase(), entry.getValue());
    }

    this.parameters = new MultivaluedHashMap<>();
    // decode the inbound netty request uri parameters.
    QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri(), charset);
    for (Map.Entry<String, List<String>> entry : queryDecoder.parameters().entrySet()) {
      this.parameters.addAll(entry.getKey(), entry.getValue());
    }

    // decode the inbound netty request body parameters.
    if (Method.POST.equals(method)) {
      CharSequence charSequence = HttpUtil.getMimeType(request);
      AsciiString mimeType = charSequence != null
          ? AsciiString.of(charSequence)
          : AsciiString.EMPTY_STRING;
      boolean isFormData = mimeType.contains(HttpHeaderValues.FORM_DATA);

      if (isFormData) {
        // decode the inbound netty request body multipart/form-data parameters.
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
            new DefaultHttpDataFactory(), request, charset);
        try {
          for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
            InterfaceHttpData.HttpDataType type = data.getHttpDataType();
            switch (type) {
              case Attribute: {
                try {
                  Attribute attribute = (Attribute) data;
                  this.parameters.add(attribute.getName(), attribute.getValue());
                } catch (IOException ignore) {
                  LOGGER.warn("Could not get attribute value");
                }
                break;
              }
              case FileUpload:
                break;
              case InternalAttribute:
                break;
              default: //nop
            }
          }
        } finally {
          decoder.destroy();
        }
      } else {
        // decode the inbound netty request body raw | form-url-encoded | octet-stream parameters.
        this.content = request.content().hasArray()
            ? request.content().array()
            : request.content().toString(charset).getBytes();
      }
    }
  }

  private URI createBaseUri(ChannelHandlerContext ctx, FullHttpRequest req) {
    final String protocol = req.protocolVersion().protocolName().toLowerCase();
    String host = req.headers().get(HOST);
    if (host == null) {
      InetSocketAddress address = (InetSocketAddress) ctx.channel().localAddress();
      host = address.getHostName() + ":" + address.getPort();
    }
    return URI.create(String.format("%s://%s/", protocol, host));
  }

  @Override
  public URI getBaseUri() {
    return baseUri;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public Method getMethod() {
    return method;
  }

  @Override
  public MultivaluedMap<String, String> getParams() {
    return parameters;
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
  public int getLength() {
    return content == null ? 0 : content.length;
  }

  @Override
  public boolean isKeepAlive() {
    return keepAlive;
  }

  @Override
  public Date getDate() {
    String header = getHeader(DATE);
    return header != null ? parseHttpDate(header) : null;
  }

  @Override
  public List<MediaType> getMediaType() {
    String header = getHeader(ACCEPT);
    return header == null || header.trim().isEmpty()
        ? Collections.singletonList(MediaType.WILDCARD)
        : Collections.unmodifiableList(Arrays.stream(header.split(","))
            .map(MediaType::valueOf).collect(Collectors.toList()));
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public Response.Builder evaluatePreconditions(EntityTag entityTag) {
    if (entityTag == null) {
      return null;
    }
    return Optional
        .ofNullable(evaluateIfMatch(entityTag))
        .orElse(evaluateIfNoneMatch(entityTag));
  }

  @Override
  public Response.Builder evaluatePreconditions(Date lastModified) {
    if (lastModified == null) {
      return null;
    }
    long lastModifiedTime = lastModified.getTime();
    return Optional
        .ofNullable(evaluateIfUnmodifiedSince(lastModifiedTime))
        .orElse(evaluateIfModifiedSince(lastModifiedTime));
  }

  @Override
  public Response.Builder evaluatePreconditions(Date lastModified, EntityTag entityTag) {
    if (lastModified == null || entityTag == null) {
      return null;
    }

    Response.Builder r = evaluateIfMatch(entityTag);
    if (r != null) {
      return r;
    }

    final long lastModifiedTime = lastModified.getTime();
    r = evaluateIfUnmodifiedSince(lastModifiedTime);
    if (r != null) {
      return r;
    }

    final boolean isGetOrHead = isGetOrHead();
    final EntityTag matchingTag = getIfNoneMatch();
    if (matchingTag != null) {
      r = evaluateIfNoneMatch(entityTag, matchingTag, isGetOrHead);
      // If the If-None-Match header is present and there is no
      // match then the If-Modified-Since header must be ignored
      if (r == null) {
        return null;
      }

      // Otherwise if the If-None-Match header is present and there
      // is a match then the If-Modified-Since header must be checked
      // for consistency
    }

    final String ifModifiedSinceHeader = getHeader(IF_MODIFIED_SINCE);
    if (ifModifiedSinceHeader != null && !ifModifiedSinceHeader.isEmpty() && isGetOrHead) {
      r = evaluateIfModifiedSince(lastModifiedTime, ifModifiedSinceHeader);
      if (r != null) {
        r.tag(entityTag);
      }
    }

    return r;
  }

  /**
   * http://docs.oracle.com/javaee/6/tutorial/doc/gkqda.html
   *
   * <p>JAX-RS provides support for conditional GET and PUT HTTP requests. Conditional GET requests
   * help save bandwidth by improving the efficiency of client processing.
   *
   * <p>A GET request can return a Not Modified (304) response if the representation has not changed
   * since the previous request. For example, a web site can return 304 responses for all its static
   * images that have not changed since the previous request.
   *
   * <p>A PUT request can return a Precondition Failed (412) response if the representation has been
   * modified since the last request. The conditional PUT can help avoid the lost update problem.
   * Conditional HTTP requests can be used with the Last-Modified and ETag headers. The
   * Last-Modified header can represent dates with granularity of one second.
   */

  @Override
  public Response.Builder evaluatePreconditions() {
    String ifMatch = getHeader(IF_MATCH);
    EntityTag matchingTag = readMatchingEntityTag(ifMatch);
    if (matchingTag == null) {
      return null;
    }
    // Since the resource does not exist the method must not be
    // perform and 412 Precondition Failed is returned
    return Response.status(Response.Status.PRECONDITION_FAILED);
  }

  private Response.Builder evaluateIfUnmodifiedSince(long lastModifiedTime) {
    String ifUnmodifiedSinceHeader = getHeader(IF_UNMODIFIED_SINCE);

    if (ifUnmodifiedSinceHeader != null && !ifUnmodifiedSinceHeader.trim().isEmpty()) {
      long ifUnmodifiedSince = parseHttpDate(ifUnmodifiedSinceHeader).getTime();
      if (roundDown(lastModifiedTime) > ifUnmodifiedSince) {
        // 412 Precondition Failed
        return Response.status(Response.Status.PRECONDITION_FAILED);
      }
    }
    return null;
  }

  private Response.Builder evaluateIfModifiedSince(long lastModifiedTime) {
    String ifModifiedSinceHeader = getHeader(IF_MODIFIED_SINCE);

    if (ifModifiedSinceHeader != null
        && !ifModifiedSinceHeader.trim().isEmpty()
        && isGetOrHead()) {
      return evaluateIfModifiedSince(lastModifiedTime, ifModifiedSinceHeader);
    }
    return null;
  }

  private Response.Builder evaluateIfModifiedSince(
      long lastModifiedTime, String ifModifiedSinceHeader) {
    final long ifModifiedSince = parseHttpDate(ifModifiedSinceHeader).getTime();
    if (roundDown(lastModifiedTime) <= ifModifiedSince) {
      // 304 Not modified
      return Response.notModified();
    }
    return null;
  }

  private long roundDown(long time) {
    // Round down the time to the nearest second
    return time - time % 1000;
  }

  private Response.Builder evaluateIfMatch(EntityTag entityTag) {
    // The strong comparison function must be used to compare the entity
    // tags. Thus if the entity tag of the entity is weak then matching
    // of entity tags in the If-Match header should fail.
    if (entityTag.isWeak()) {
      return Response.status(Response.Status.PRECONDITION_FAILED);
    }

    EntityTag matchingTag = getIfMatch();

    if (matchingTag != null && matchingTag != EntityTag.ANY_MATCH
        && !matchingTag.equals(entityTag)) {
      return Response.status(Response.Status.PRECONDITION_FAILED);
    }
    return null;
  }

  private Response.Builder evaluateIfNoneMatch(EntityTag entityTag) {
    EntityTag matchingTag = getIfNoneMatch();

    if (matchingTag != null) {
      return evaluateIfNoneMatch(entityTag, matchingTag, isGetOrHead());
    }
    return null;
  }

  private Response.Builder evaluateIfNoneMatch(
      EntityTag entityTag, EntityTag matchingTag, boolean isGetOrHead) {
    if (isGetOrHead) {
      // The weak comparison function may be used to compare entity tags
      if (EntityTag.ANY_MATCH.equals(matchingTag) || matchingTag.equals(entityTag)
          || matchingTag.equals(new EntityTag(entityTag.getValue(), !entityTag.isWeak()))) {
        // 304 Not Modified
        return Response.notModified(entityTag);
      }
    } else {
      // The strong comparison function must be used to compare the entity
      // tags. Thus if the entity tag of the entity is weak then matching
      // of entity tags in the If-None-Match header should fail if the
      // HTTP method is not GET or not HEAD.
      if (entityTag.isWeak()) {
        return null;
      }

      if (EntityTag.ANY_MATCH.equals(matchingTag) || matchingTag.equals(entityTag)) {
        // 412 Precondition Failed
        return Response.status(Response.Status.PRECONDITION_FAILED);
      }
    }
    return null;
  }

  private EntityTag getIfMatch() {
    return readMatchingEntityTag(getHeader(IF_MATCH));
  }

  private EntityTag getIfNoneMatch() {
    return readMatchingEntityTag(getHeader(IF_NONE_MATCH));
  }

  private boolean isGetOrHead() {
    HttpMethod httpMethod = HttpMethod.valueOf(getMethod().toString());
    return HttpMethod.GET.equals(httpMethod) || HttpMethod.HEAD.equals(httpMethod);
  }

  private EntityTag readMatchingEntityTag(String header) {
    return header == null || header.isEmpty()
        ? null
        : EntityTag.valueOf(header);
  }

  @Override
  public Version getVersion() {
    return version;
  }

  @Override
  public MultivaluedMap<String, String> getHeaders() {
    return headers;
  }

}

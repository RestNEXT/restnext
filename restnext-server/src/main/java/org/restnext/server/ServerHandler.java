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

package org.restnext.server;

import static org.restnext.core.http.Response.Status.BAD_REQUEST;
import static org.restnext.core.http.Response.Status.INTERNAL_SERVER_ERROR;
import static org.restnext.core.http.Response.Status.METHOD_NOT_ALLOWED;
import static org.restnext.core.http.Response.Status.NOT_FOUND;
import static org.restnext.core.http.Response.Status.UNAUTHORIZED;
import static org.restnext.core.http.Response.Status.UNSUPPORTED_MEDIA_TYPE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.internal.ThrowableUtil;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.restnext.core.http.MediaType;
import org.restnext.core.http.Message;
import org.restnext.core.http.Request;
import org.restnext.core.http.RequestImpl;
import org.restnext.core.http.Response;
import org.restnext.core.url.UrlMatch;
import org.restnext.route.Route;
import org.restnext.security.Security;

/**
 * Created by thiago on 04/08/16.
 */
@ChannelHandler.Sharable
class ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  public static final ServerHandler INSTANCE = new ServerHandler();

  private ServerHandler() {

  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
    if (req.decoderResult().isFailure()) {
      throw new ServerException(req.decoderResult().cause(), BAD_REQUEST);
    }

    // Handle 100 - Continue request.
    if (HttpUtil.is100ContinueExpected(req)) {
      ctx.write(new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.CONTINUE));
    }

    // Create Request from FullHttpRequest
    final Request request = new RequestImpl(ctx, req);
    final String uri = request.getUri().toString();
    final URI baseUri = request.getBaseUri();
    final URI fullRequestUri = baseUri.resolve(uri);
    final List<MediaType> medias = request.getMediaType();
    final Request.Method method = request.getMethod();

    // Check security constraint for the request,
    // otherwise return 401 - Unauthorized  response.
    if (!Security.checkAuthorization(request)) {
      throw new ServerException(String.format(
          "Access denied for the uri %s", fullRequestUri), UNAUTHORIZED);
    }

    // Get registered route mapping for the request uri, otherwise return 404 - Not Found  response.
    Route.Mapping routeMapping = Optional.ofNullable(Route.INSTANCE.getRouteMapping(uri))
        .filter(Route.Mapping::isEnable)
        .filter(mapping -> mapping.getRouteProvider() != null)
        .orElseThrow(() -> new ServerException(String.format(
            "Route mapping not found for the method %s and uri %s", method, fullRequestUri),
            NOT_FOUND));

    // Parse the uri parameters and add it to request parameters map.
    UrlMatch urlMatch = routeMapping.getUrlMatcher().match(uri);
    for (Map.Entry<String, String> entry : urlMatch.parameterSet()) {
      request.getParams().add(entry.getKey(), entry.getValue());
    }

    // Check if the registered route mapping methods contains the request method,
    // otherwise return 405 - Method Not Allowed response.
    Optional.ofNullable(routeMapping.getMethods())
        .filter(methods -> methods.contains(method) || methods.isEmpty())
        .orElseThrow(() -> new ServerException(String.format(
            "Method %s not allowed for the request uri %s", method, fullRequestUri),
            METHOD_NOT_ALLOWED));

    // Check if the registered route mapping medias contains the request media,
    // otherwise return 415 Unsupported Media Type response.
    Optional.ofNullable(routeMapping.getMedias())
        .filter(routeMedias -> anyMatchMediaType(routeMedias, medias))
        .orElseThrow(() -> new ServerException(String.format(
            "Unsupported %s media type(s) for the request uri %s", medias, fullRequestUri),
            UNSUPPORTED_MEDIA_TYPE));

    // Write the response for the request.
    write(ctx, Optional.ofNullable(routeMapping.writeResponse(request))
        .orElse(Response.noContent().build()), request.isKeepAlive());
  }

  private void write(ChannelHandlerContext ctx, Response response, boolean keepAlive) {
    HttpVersion version = fromVersion(response.getVersion());
    HttpResponseStatus status = fromStatus(response.getStatus());
    ByteBuf content = response.hasContent()
        ? Unpooled.wrappedBuffer(response.getContent())
        : Unpooled.EMPTY_BUFFER;

    boolean chunked = response.isChunked();

    // create netty response
    HttpResponse resp;
    HttpChunkedInput chunkedResp = chunked
        ? new HttpChunkedInput(new ChunkedStream(
            new ByteBufInputStream(content), response.getChunkSize()))
        : null;

    if (chunked) {
      resp = new DefaultHttpResponse(version, status);
      HttpUtil.setTransferEncodingChunked(resp, chunked);
      createOutboutHeaders(resp, response, keepAlive);
      // Write the initial line and the header.
      ctx.write(resp);
    } else {
      resp = new DefaultFullHttpResponse(version, status, content);
      createOutboutHeaders(resp, response, keepAlive);
    }

    if (keepAlive) {
      if (chunked) {
        ctx.write(chunkedResp);
      } else {
        HttpUtil.setContentLength(resp, content.readableBytes());
        ctx.write(resp);
      }
    } else {
      ChannelFuture channelFuture;
      if (chunked) {
        channelFuture = ctx.writeAndFlush(chunkedResp);
      } else {
        channelFuture = ctx.writeAndFlush(resp);
      }
      // Close the connection after the write operation is done if necessary.
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void createOutboutHeaders(HttpResponse resp, Response response, boolean keepAlive) {
    // Copy the outbound response headers.
    for (Map.Entry<String, List<String>> entries : response.getHeaders().entrySet()) {
      resp.headers().add(entries.getKey(), entries.getValue());
    }
    // Check and set keep alive header to decide
    // whether to close the connection or not.
    HttpUtil.setKeepAlive(resp, keepAlive);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (ctx.channel().isActive()) {
      // Create the response status error.
      Response.Status status = cause instanceof ServerException
          ? ((ServerException) cause).getResponseStatus()
          : INTERNAL_SERVER_ERROR;

      // Create the response error body.
      String newLine = "\r\n";
      StringJoiner content = new StringJoiner(newLine);
      content.add("statusCode: " + status.getStatusCode());
      content.add("statusMessage: " + status.getReasonPhrase());
      content.add("statusFamily: " + status.getFamily());
      if (cause.getMessage() != null) {
        content.add("errorMessage: " + cause.getMessage());
      }
      if (cause.getCause() != null) {
        content.add("detailErrorMessage: " + cause.getCause().getMessage());
      }
      content.add("stackTraceMessage: " + ThrowableUtil.stackTraceToString(cause));

      // Write the response error.
      write(ctx, Response
          .status(status)
          .content(content.toString())
          .type(MediaType.TEXT_UTF8)
          .build(), false);
    }
    ctx.close();
  }

  private HttpVersion fromVersion(Message.Version version) {
    switch (version) {
      case HTTP_1_0: return HttpVersion.HTTP_1_0;
      case HTTP_1_1: return HttpVersion.HTTP_1_1;
      default: return HttpVersion.HTTP_1_1;
    }
  }

  private HttpResponseStatus fromStatus(Response.Status status) {
    switch (status) {
      case OK: return HttpResponseStatus.OK;
      case CREATED: return HttpResponseStatus.CREATED;
      case ACCEPTED: return HttpResponseStatus.ACCEPTED;
      case NO_CONTENT: return HttpResponseStatus.NO_CONTENT;
      case RESET_CONTENT: return HttpResponseStatus.RESET_CONTENT;
      case PARTIAL_CONTENT: return HttpResponseStatus.PARTIAL_CONTENT;
      case MOVED_PERMANENTLY: return HttpResponseStatus.MOVED_PERMANENTLY;
      case FOUND: return HttpResponseStatus.FOUND;
      case SEE_OTHER: return HttpResponseStatus.SEE_OTHER;
      case NOT_MODIFIED: return HttpResponseStatus.NOT_MODIFIED;
      case USE_PROXY: return HttpResponseStatus.USE_PROXY;
      case TEMPORARY_REDIRECT: return HttpResponseStatus.TEMPORARY_REDIRECT;
      case BAD_REQUEST: return HttpResponseStatus.BAD_REQUEST;
      case UNAUTHORIZED: return HttpResponseStatus.UNAUTHORIZED;
      case PAYMENT_REQUIRED: return HttpResponseStatus.PAYMENT_REQUIRED;
      case FORBIDDEN: return HttpResponseStatus.FORBIDDEN;
      case NOT_FOUND: return HttpResponseStatus.NOT_FOUND;
      case METHOD_NOT_ALLOWED: return HttpResponseStatus.METHOD_NOT_ALLOWED;
      case NOT_ACCEPTABLE: return HttpResponseStatus.NOT_ACCEPTABLE;
      case PROXY_AUTHENTICATION_REQUIRED: return HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;
      case REQUEST_TIMEOUT: return HttpResponseStatus.REQUEST_TIMEOUT;
      case CONFLICT: return HttpResponseStatus.CONFLICT;
      case GONE: return HttpResponseStatus.GONE;
      case LENGTH_REQUIRED: return HttpResponseStatus.LENGTH_REQUIRED;
      case PRECONDITION_FAILED: return HttpResponseStatus.PRECONDITION_FAILED;
      case REQUEST_ENTITY_TOO_LARGE: return HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;
      case REQUEST_URI_TOO_LONG: return HttpResponseStatus.REQUEST_URI_TOO_LONG;
      case UNSUPPORTED_MEDIA_TYPE: return HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE;
      case REQUESTED_RANGE_NOT_SATISFIABLE:
        return HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      case EXPECTATION_FAILED: return HttpResponseStatus.EXPECTATION_FAILED;
      case INTERNAL_SERVER_ERROR: return HttpResponseStatus.INTERNAL_SERVER_ERROR;
      case NOT_IMPLEMENTED: return HttpResponseStatus.NOT_IMPLEMENTED;
      case BAD_GATEWAY: return HttpResponseStatus.BAD_GATEWAY;
      case SERVICE_UNAVAILABLE: return HttpResponseStatus.SERVICE_UNAVAILABLE;
      case GATEWAY_TIMEOUT: return HttpResponseStatus.GATEWAY_TIMEOUT;
      case HTTP_VERSION_NOT_SUPPORTED: return HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED;
      case CONTINUE: return HttpResponseStatus.CONTINUE;
      default: throw new RuntimeException(String.format("Status: %s not supported", status));
    }
  }

  private boolean anyMatchMediaType(List<MediaType> routeMappingMedias,
                                    List<MediaType> requestMedias) {

    if (routeMappingMedias == null || routeMappingMedias.isEmpty() || requestMedias == null
        || requestMedias.isEmpty() || requestMedias.contains(MediaType.WILDCARD)) {
      return true;
    }

    boolean r = false;
    for (MediaType e : routeMappingMedias) {
      if (r) {
        break;
      }
      for (MediaType e2 : requestMedias) {
        r = e.isSimilar(e2) || e.isCompatible(e2);
        if (r) {
          break;
        }
      }
    }
    return r;
  }
}

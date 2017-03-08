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

import static org.restnext.core.http.codec.Response.Status.BAD_REQUEST;
import static org.restnext.core.http.codec.Response.Status.INTERNAL_SERVER_ERROR;
import static org.restnext.core.http.codec.Response.Status.METHOD_NOT_ALLOWED;
import static org.restnext.core.http.codec.Response.Status.NOT_FOUND;
import static org.restnext.core.http.codec.Response.Status.UNAUTHORIZED;
import static org.restnext.core.http.codec.Response.Status.UNSUPPORTED_MEDIA_TYPE;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.internal.ThrowableUtil;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.restnext.core.http.MediaType;
import org.restnext.core.http.codec.Request;
import org.restnext.core.http.codec.Response;
import org.restnext.core.http.url.UrlMatch;
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
    if (!req.decoderResult().isSuccess()) {
      throw new ServerException(BAD_REQUEST);
    }

    // Handle 100 - Continue request.
    if (HttpUtil.is100ContinueExpected(req)) {
      ctx.write(new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.CONTINUE));
    }

    // Create Request from FullHttpRequest
    final Request request = Request.fromRequest(ctx, req);
    final String uri = request.getUri().toString();
    final URI baseUri = request.getBaseUri();
    final URI fullRequestUri = baseUri.resolve(uri);
    final MediaType media = request.getMediaType();
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
        .filter(_routeMapping -> _routeMapping.getRouteProvider() != null)
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
        .filter(medias -> medias.contains(media) || medias.isEmpty() || media == null)
        .orElseThrow(() -> new ServerException(String.format(
            "Unsupported %s media type for the request uri %s", media, fullRequestUri),
            UNSUPPORTED_MEDIA_TYPE));

    // Write the response for the request.
    write(ctx, Optional.ofNullable(routeMapping.writeResponse(request))
        .orElse(Response.noContent().build()), request.isKeepAlive());
  }

  private void write(ChannelHandlerContext ctx, Response response, boolean keepAlive) {
    FullHttpResponse resp = response.getFullHttpResponse();

    // Check and set keep alive header to decide
    // whether to close the connection or not.
    HttpUtil.setKeepAlive(resp, keepAlive);

    if (keepAlive) {
      HttpUtil.setContentLength(resp, resp.content().readableBytes());
      ctx.write(resp);
    } else {
      // Close the connection after the write operation is done if necessary.
      ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
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
      StringJoiner content = new StringJoiner(newLine)
          .add("statusCode: " + status.getStatusCode())
          .add("statusMessage: " + status.getReasonPhrase())
          .add("statusFamily: " + status.getFamily());
      if (cause.getMessage() != null) {
        content.add("errorMessage: " + cause.getMessage());
      }
      if (cause.getCause() != null) {
        content.add("detailErrorMessage: " + cause.getCause().getMessage());
      }
      content.add("stackTraceMessage: " + ThrowableUtil.stackTraceToString(cause));

      // Write the response error.
      write(ctx, Response.status(status)
              .content(content.toString())
              .type(MediaType.TEXT_UTF8)
              .build(),
          false);
    }
    ctx.close();
  }

}

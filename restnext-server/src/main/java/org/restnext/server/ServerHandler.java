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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.restnext.core.http.MediaType;
import org.restnext.core.http.codec.Message;
import org.restnext.core.http.codec.Request;
import org.restnext.core.http.codec.Response;
import org.restnext.core.http.url.UrlMatch;
import org.restnext.route.Route;
import org.restnext.security.Security;
import org.restnext.util.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.restnext.core.http.codec.Response.Status.*;

/**
 * Created by thiago on 04/08/16.
 */
class ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

//    private final EventExecutorGroup eventExecutors = new DefaultEventExecutorGroup(30);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if (!req.decoderResult().isSuccess()) {
            throw new ServerException(BAD_REQUEST);
        }
        // Handle 100 - Continue request.
        if (HttpUtil.is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.CONTINUE));
        }

        // Decode fullHttpRequest to Request object.
        final Request request = Request.fromRequest(ctx, req);
        final String uri = request.getURI();
        final MediaType media = request.getMediaType();
        final boolean keepAlive = request.isKeepAlive();
        final Request.Method method = request.getMethod();
        final Message.Version version = request.getVersion();

        // Check security constraint for the request,
        // otherwise return 401 - Unauthorized  response.
        if (!Security.checkAuthorization(request)) {
            throw new ServerException(String.format(
                    "Access denied for the uri %s", uri), UNAUTHORIZED);
        }

        // Get registered route mapping for the request uri, otherwise return 404 - Not Found  response.
        Route.Mapping routeMapping = Optional.ofNullable(Route.INSTANCE.getRouteMapping(uri))
                .filter(Route.Mapping::isEnable)
                .filter(_routeMapping -> _routeMapping.getRouteProvider() != null)
                .orElseThrow(() -> new ServerException(String.format(
                        "Route mapping not found for the method %s and uri %s", method, uri), NOT_FOUND));

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
                        "Method %s not allowed for the request uri %s", method, uri), METHOD_NOT_ALLOWED));

        // Check if the registered route mapping medias contains the request media,
        // otherwise return 415 Unsupported Media Type response.
        Optional.ofNullable(routeMapping.getMedias())
                .filter(medias -> medias.contains(media) || medias.isEmpty() || media == null)
                .orElseThrow(() -> new ServerException(String.format(
                        "Unsupported %s media type for the request uri %s", media, uri), UNSUPPORTED_MEDIA_TYPE));

        // Write sync the response for the request.
        write(ctx, Optional.ofNullable(routeMapping.writeResponse(request))
                .orElse(Response.noContent().version(version).build()), keepAlive);

        ////////////////////

//        // Create a response task to get the response for the request.
//        Callable<Response> responseTask = () -> Optional.ofNullable(routeMapping.writeResponse(request))
//                .orElse(Response.noContent().version(version).build());
//
//        // Submit the response task and lister to write a response
//        // when the operation complete.
//        eventExecutors.submit(responseTask).addListener(future -> {
//            if (future.isSuccess()) {
//                // Write async the response for the request.
//                write(ctx, (Response) future.get(), keepAlive);
//            }
////            else {
////                exceptionCaught(ctx, future.cause());
////            }
//        });
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            // Create the response status error.
            Response.Status status = cause instanceof ServerException ?
                    ((ServerException) cause).getResponseStatus() : INTERNAL_SERVER_ERROR;

            // Create the response error body.
            Map<String, Object> map = new HashMap<>();
            map.put("statusCode", status.getStatusCode());
            map.put("statusMessage", status.getReasonPhrase());
            map.put("statusFamily", status.getFamily());
            if (cause.getMessage() != null) map.put("errorMessage", cause.getMessage());
            if (cause.getCause() != null) map.put("detailErrorMessage", cause.getCause().getMessage());
//            map.put("developerErrorMessage", ExceptionUtils.getStackTraceAsString(cause));

            // Build the response error.
            Response response = Response.status(status).content(JsonUtils.toJsonAsBytes(map)).type(MediaType.JSON_UTF8).build();

            // Write the response error.
            write(ctx, response, false);
        }
        ctx.close();
    }

    private void write(ChannelHandlerContext ctx, Response response, boolean keepAlive) {
        Objects.requireNonNull(response);

        // Get the response as FullHttpResponse object.
        FullHttpResponse resp = response.getFullHttpResponse();

        // Check and set keep alive header to decide
        // whether to close the connection or not.
        HttpUtil.setKeepAlive(resp, keepAlive);

        if (keepAlive) {
            HttpUtil.setContentLength(resp, resp.content().readableBytes());
            // Write the response.
            ctx.writeAndFlush(resp);
        } else {
            // Close the connection after the write operation is done if necessary.
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        }
    }

}

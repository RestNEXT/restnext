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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.restnext.core.http.codec.Request;
import org.restnext.core.http.codec.Response;
import org.restnext.route.Route;
import org.restnext.route.RouteScanner;
import org.restnext.security.Security;
import org.restnext.security.SecurityScanner;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by thiago on 04/08/16.
 */
public final class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final CorsConfig corsConfig;
    private final int maxContentLength;
    private final InetSocketAddress bindAddress;

    private ServerInitializer(final Builder builder) {
        this.sslCtx = builder.sslContext;
        this.corsConfig = builder.corsConfig;
        this.maxContentLength = builder.maxContentLength;
        this.bindAddress = builder.bindAddress;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()));
        pipeline.addLast("http", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
        pipeline.addLast("streamer", new ChunkedWriteHandler());
        if (corsConfig != null) ch.pipeline().addLast("cors", new CorsHandler(corsConfig));
        pipeline.addLast("handler", new ServerHandler());
    }

    // getters methods

    public SslContext getSslCtx() {
        return sslCtx;
    }

    public CorsConfig getCorsConfig() {
        return corsConfig;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    // static methods

    public static Builder builder() {
        return new ServerInitializer.Builder();
    }

    public static Builder route(String uri, Function<Request, Response> provider) {
        return builder().route(uri, provider);
    }

    @SafeVarargs
    public static Builder routes(Route.Mapping... routesMapping) {
        return builder().routes(routesMapping);
    }

    // inner builder class

    public static final class Builder {

        private int maxContentLength = 64 * 1024;
        private CorsConfig corsConfig = getCorsConfig();
        private SslContext sslContext /*= getSslContext()*/;
        private InetSocketAddress bindAddress = new InetSocketAddress(8080);

        public Builder bindAddress(InetSocketAddress bindAddress) {
            this.bindAddress = bindAddress;
            return this;
        }

        public Builder maxContentLength(int maxContentLength) {
            this.maxContentLength = maxContentLength;
            return this;
        }

        public Builder cors(CorsConfig corsConfig) {
            this.corsConfig = corsConfig;
            return this;
        }

        public Builder ssl(SslContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder route(String uri, Function<Request, Response> provider) {
            return routes(Route.Mapping.uri(uri, provider).build());
        }

        @SafeVarargs
        public final Builder routes(Route.Mapping... routeMapping) {
            Arrays.asList(routeMapping).forEach(Route.INSTANCE::register);
            return this;
        }

        public Builder secure(String uri, Function<Request, Boolean> provider) {
            return secures(Security.Mapping.uri(uri, provider).build());
        }

        @SafeVarargs
        public final Builder secures(Security.Mapping... securityMapping) {
            Arrays.asList(securityMapping).forEach(Security.INSTANCE::register);
            return this;
        }

        public Builder enableSecurityRoutesScan() {
            return enableSecurityRoutesScan(SecurityScanner.DEFAULT_SECURITY_DIR);
        }

        public Builder enableSecurityRoutesScan(Path securityDirectory) {
            SecurityScanner securityScanner = new SecurityScanner(Security.INSTANCE, securityDirectory);
            securityScanner.scan();
            return this;
        }

        public Builder enableRoutesScan() {
            return enableRoutesScan(RouteScanner.DEFAULT_ROUTE_DIR);
        }

        public Builder enableRoutesScan(Path routeDirectory) {
            RouteScanner routeScanner = new RouteScanner(Route.INSTANCE, routeDirectory);
            routeScanner.scan();
            return this;
        }

        public ServerInitializer build() {
            // build and register default health check route.
            route("/healthcheck", request -> Response.ok().version(request.getVersion()).build());
            return new ServerInitializer(this);
        }

        // convenient methods

        public Server start() {
            Server server = new Server(build());
            server.start();
            return server;
        }

        private SslContext getSslContext() {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate("restnext.org");
                return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (CertificateException | SSLException ignore) {
                return null;
            }
        }

        private CorsConfig getCorsConfig() {
            return CorsConfigBuilder.forAnyOrigin()
                    .allowNullOrigin()
                    .allowCredentials()
                    .build();
        }
    }
}

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
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.net.ssl.SSLException;

import org.restnext.core.http.Request;
import org.restnext.core.http.Response;
import org.restnext.route.Route;
import org.restnext.route.RouteScanner;
import org.restnext.security.Security;
import org.restnext.security.SecurityScanner;

/**
 * Created by thiago on 04/08/16.
 */
public final class ServerInitializer extends ChannelInitializer<SocketChannel> {

  private final Timeout timeout;
  private final SslContext sslCtx;
  private final CorsConfig corsConfig;
  private final int maxContentLength;
  private final InetSocketAddress bindAddress;
  private final EventExecutorGroup group;

  private ServerInitializer(final Builder builder) {
    this(builder, true);
  }

  private ServerInitializer(final Builder builder, final boolean daemon) {
    this.sslCtx = builder.sslContext;
    this.corsConfig = builder.corsConfig;
    this.maxContentLength = builder.maxContentLength;
    this.bindAddress = builder.bindAddress;
    this.timeout = builder.timeout;

    ThreadFactory threadFactory = daemon
        ? new DaemonThreadFactory(Executors.defaultThreadFactory())
        : Executors.defaultThreadFactory();
    this.group = new DefaultEventExecutorGroup(
        Runtime.getRuntime().availableProcessors() * 2,
        threadFactory);
  }

  public static Builder route(String uri, Function<Request, Response> provider) {
    return builder().route(uri, provider);
  }

  // getters methods

  public static Builder builder() {
    return new ServerInitializer.Builder();
  }

  public static Builder routes(Route.Mapping... routesMapping) {
    return builder().routes(routesMapping);
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslCtx != null) {
      pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()));
    }
    pipeline.addLast("http", new HttpServerCodec());
    pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
    pipeline.addLast("streamer", new ChunkedWriteHandler());
    if (corsConfig != null) {
      ch.pipeline().addLast("cors", new CorsHandler(corsConfig));
    }
    if (timeout != null) {
      pipeline.addLast("timeout", new ReadTimeoutHandler(timeout.amount, timeout.unit));
    }
    // Tell the pipeline to run MyBusinessLogicHandler's event handler methods in a different
    // thread than an I/O thread so that the I/O thread is not blocked by a time-consuming task.
    // If your business logic is fully asynchronous or finished very quickly, you don't need to
    // specify a group.
    pipeline.addLast(group, "handler", ServerHandler.INSTANCE);
  }

  public CorsConfig getCorsConfig() {
    return corsConfig;
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }

  // support methods

  public InetSocketAddress getBindAddress() {
    return bindAddress;
  }

  // static methods

  public Timeout getTimeout() {
    return timeout;
  }

  public boolean isSslConfigured() {
    return getSslCtx() != null;
  }

  public SslContext getSslCtx() {
    return sslCtx;
  }

  // inner support class

  static final class Timeout {
    private final long amount;
    private final TimeUnit unit;

    Timeout() {
      this(30, TimeUnit.SECONDS);
    }

    Timeout(long amount, TimeUnit unit) {
      this.amount = amount;
      this.unit = unit;
    }
  }

  static class DaemonThreadFactory implements ThreadFactory {
    private final ThreadFactory threadFactory;

    DaemonThreadFactory(ThreadFactory threadFactory) {
      this.threadFactory = threadFactory;
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread thread = threadFactory.newThread(r);
      if (thread != null) {
        thread.setDaemon(true);
      }
      return thread;
    }
  }

  // inner builder class

  public static final class Builder {

    public static final ServerCertificate DEFAULT_SERVER_CERTIFICATE =
        new ServerSelfSignedCertificate();

    private int maxContentLength = 64 * 1024;
    private Timeout timeout = new Timeout();
    private CorsConfig corsConfig /*= getCorsConfig()*/;
    private SslContext sslContext;
    private InetSocketAddress bindAddress = new InetSocketAddress(8080);

    public Builder bindAddress(InetSocketAddress bindAddress) {
      this.bindAddress = bindAddress;
      return this;
    }

    public Builder timeout(int timeout, TimeUnit unit) {
      this.timeout = new Timeout(timeout, unit);
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

    public Builder ssl() {
      return ssl(DEFAULT_SERVER_CERTIFICATE);
    }

    public Builder ssl(ServerCertificate certificate) {
      return ssl(createSslContext(certificate));
    }

    public Builder ssl(SslContext sslContext) {
      this.sslContext = sslContext;
      return this;
    }

    private SslContext createSslContext(ServerCertificate serverCertificate) {
      try {
        return SslContextBuilder.forServer(serverCertificate.getCertificate(),
            serverCertificate.getPrivateKey()).build();
      } catch (SSLException ignore) {
        return null;
      }
    }

    public Builder secure(String uri, Function<Request, Boolean> provider) {
      return secures(Security.Mapping.uri(uri, provider).build());
    }

    public final Builder secures(Security.Mapping... securityMapping) {
      Arrays.asList(securityMapping).forEach(Security.INSTANCE::register);
      return this;
    }

    public Builder enableSecurityRoutesScan() {
      return enableSecurityRoutesScan(SecurityScanner.DEFAULT_SECURITY_DIR);
    }

    /**
     * Enable the security route scan approach.
     *
     * @param securityDirectory the security directory to scan
     * @return server initializer builder
     */
    public Builder enableSecurityRoutesScan(Path securityDirectory) {
      SecurityScanner securityScanner = new SecurityScanner(Security.INSTANCE, securityDirectory);
      securityScanner.scan();
      return this;
    }

    public Builder enableRoutesScan() {
      return enableRoutesScan(RouteScanner.DEFAULT_ROUTE_DIR);
    }

    /**
     * Enable the route scan approach.
     *
     * @param routeDirectory the route directory to scan
     * @return server initializer builder
     */
    public Builder enableRoutesScan(Path routeDirectory) {
      RouteScanner routeScanner = new RouteScanner(Route.INSTANCE, routeDirectory);
      routeScanner.scan();
      return this;
    }

    /**
     * Shortcut to start the server.
     *
     * @return the started server
     */
    public Server start() {
      Server server = new Server(build());
      server.start();
      return server;
    }

    /**
     * Build the server initializer.
     *
     * @return the server initializer
     */
    public ServerInitializer build() {
      // register default health check route.
      route("/ping", request -> ServerResponse.ok("pong").build());
      return new ServerInitializer(this);
    }

    // convenient methods

    public Builder route(String uri, Function<Request, Response> provider) {
      return routes(Route.Mapping.uri(uri, provider).build());
    }

    public final Builder routes(Route.Mapping... routeMapping) {
      Arrays.asList(routeMapping).forEach(Route.INSTANCE::register);
      return this;
    }

    private CorsConfig getCorsConfig() {
      return CorsConfigBuilder.forAnyOrigin()
          .allowNullOrigin()
          .allowCredentials()
          .build();
    }

    private static class ServerSelfSignedCertificate implements ServerCertificate {

      static final String DEFAULT_FQND = "restnext.org";

      private SelfSignedCertificate certificate;

      public ServerSelfSignedCertificate() {
        this(DEFAULT_FQND);
      }

      /**
       * Creates a new instance.
       *
       * @param fqdn a fully qualified domain name
       */
      public ServerSelfSignedCertificate(String fqdn) {
        try {
          certificate = new SelfSignedCertificate(fqdn);
        } catch (CertificateException ignore) {
          // nop
        }
      }

      @Override
      public InputStream getCertificate() {
        try {
          return Files.newInputStream(certificate.certificate().toPath());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public InputStream getPrivateKey() {
        try {
          return Files.newInputStream(certificate.privateKey().toPath());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

  }
}

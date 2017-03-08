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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thiago on 04/08/16.
 */
public final class Server {

  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
  private final ServerInitializer serverInitializer;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public Server(final ServerInitializer serverInitializer) {
    this(true, serverInitializer);
  }

  /**
   * Constructor with auto start flag and the server initializer.
   *
   * @param autoStart true to automatic start the server
   * @param serverInitializer the server initializer
   */
  public Server(final boolean autoStart, final ServerInitializer serverInitializer) {
    Objects.requireNonNull(serverInitializer, "Server initializer must not be null");
    this.serverInitializer = serverInitializer;
    Runtime.getRuntime().addShutdownHook(new Thread("server-hook") {
      @Override
      public void run() {
        LOGGER.info("Running JVM shutdown hook to stop the server.");
        Server.this.stop();
      }
    });
    if (autoStart) {
      start();
    }
  }

  /**
   * Starts the server.
   */
  public void start() {
    loadAndPrintBanner();
    try {
      InetSocketAddress bindAddress = serverInitializer.getBindAddress();
      ServerBootstrap serverBootstrap = Epoll.isAvailable()
          ? newEpoolServerBootstrap()
          : newNioServerBootstrap();
      ChannelFuture channelFuture = serverBootstrap
          //.handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(serverInitializer)
          .bind(bindAddress)
          .sync();
      LOGGER.info("Application is running at - {}://{}",
          (serverInitializer.isSslConfigured() ? "https" : "http"), bindAddress);
      channelFuture.channel().closeFuture().sync();
    } catch (Exception e) {
      throw new ServerException("Could not start the server", e);
    } finally {
      stop();
    }
  }

  public void stop() {
    stop(false);
  }

  private void stop(final boolean await) {
    if (bossGroup != null && workerGroup != null) {
      Future<?> futureWorkerShutdown = workerGroup.shutdownGracefully();
      Future<?> futureBossShutdown = bossGroup.shutdownGracefully();
      if (await) {
        futureWorkerShutdown.awaitUninterruptibly();
        futureBossShutdown.awaitUninterruptibly();
      }
    }
  }

  private void loadAndPrintBanner() {
    String target = "banner-logo.txt";
    URL url = Server.class.getClassLoader().getResource(target);
    URI uri = url != null ? URI.create(url.toString()) : null;
    if (uri != null) {
      String scheme = uri.getScheme();
      switch (scheme) {
        case "file":
          printBanner(Paths.get(uri));
          break;
        case "jar":
          try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            printBanner(fs.getPath(target));
          } catch (IOException ignore) {
            //nop
          }
          break;
        default: //nop
      }
    }
  }

  private ServerBootstrap newEpoolServerBootstrap() {
    bossGroup = new EpollEventLoopGroup();
    workerGroup = new EpollEventLoopGroup();
    return new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(EpollServerSocketChannel.class);
  }

  private ServerBootstrap newNioServerBootstrap() {
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    return new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class);
  }

  private void printBanner(Path path) {
    if (path != null && Files.exists(path)) {
      try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
        final Properties props = new Properties();
        ClassLoader classLoader = Server.class.getClassLoader();
        try (InputStream mavenProps = classLoader.getResourceAsStream(
                     "META-INF/maven/org.restnext/restnext-server/pom.properties")) {
          if (mavenProps != null) {
            props.load(mavenProps);
          }
        }
        stream
            .map(s -> String.format(s, props.getProperty("version", "")))
            .forEach(LOGGER::info);
      } catch (IOException ignore) {
        //nop
      }
    }
  }
}

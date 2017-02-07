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
import org.restnext.core.http.MediaType;
import org.restnext.core.http.codec.Request;
import org.restnext.core.http.codec.Response;
import org.restnext.util.AnsiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by thiago on 04/08/16.
 */
public final class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private final ServerInitializer serverInitializer;

    public Server(final ServerInitializer serverInitializer) {
        this(true, serverInitializer);
    }

    public Server(final boolean autoStart, final ServerInitializer serverInitializer) {
        Objects.requireNonNull(serverInitializer, "Server initializer must not be null");
        this.serverInitializer = serverInitializer;
        Runtime.getRuntime().addShutdownHook(new Thread("server-hook") {
            @Override
            public void run() {
                LOG.info("Running JVM shutdown hook to stop the server.");
                Server.this.stop();
            }
        });
        if (autoStart) start();
    }

    public static void main(String[] args) {
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

        Function<Request, Response> provider = r -> {
            // simulate long response process
//            try {
//                timeUnit.sleep(3);
//            } catch (InterruptedException e) {
//                //nop
//            }
            return Response.ok("it works", MediaType.TEXT).build();
        };

        ServerInitializer.route("/", provider).timeout(10, timeUnit).start();
    }

    public void start() {
        AnsiUtils.install();
        loadAndPrintBanner();
        try {
            boolean isSsl = serverInitializer.getSslCtx() != null;
            InetSocketAddress bindAddress = serverInitializer.getBindAddress();
            ServerBootstrap serverBootstrap = Epoll.isAvailable() ? newEpoolServerBootstrap() : newNioServerBootstrap();
            ChannelFuture channelFuture = serverBootstrap
//                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(serverInitializer)
                    .bind(bindAddress)
                    .sync();
            LOG.info("Application is running at - {}://{}", (isSsl ? "https" : "http"), bindAddress);
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new ServerException("Could not start the server", e);
        } finally {
            stop();
            AnsiUtils.uninstall();
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

    private ServerBootstrap newNioServerBootstrap() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class);
    }

    private ServerBootstrap newEpoolServerBootstrap() {
        bossGroup = new EpollEventLoopGroup();
        workerGroup = new EpollEventLoopGroup();
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(EpollServerSocketChannel.class);
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
                    } catch (IOException ignore) {}
                    break;
                default: // nop
            }
        }
    }

    private void printBanner(Path path) {
        if (path != null && Files.exists(path)) {
            try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
                stream.forEach(line -> System.out.println(AnsiUtils.error(line)));
            } catch (IOException ignore) {}
        }
    }
}

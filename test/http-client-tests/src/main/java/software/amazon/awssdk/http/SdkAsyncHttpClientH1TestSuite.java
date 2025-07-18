/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * A set of tests validating that the functionality implemented by a {@link SdkAsyncHttpClient} for HTTP/1 requests
 *
 * This is used by an HTTP plugin implementation by extending this class and implementing the abstract methods to provide this
 * suite with a testable HTTP client implementation.
 */
public abstract class SdkAsyncHttpClientH1TestSuite {
    private Server server;
    private SdkAsyncHttpClient client;

    protected abstract SdkAsyncHttpClient setupClient();

    @BeforeEach
    public void setup() throws Exception {
        server = new Server();
        server.init();

        this.client = setupClient();
    }

    @AfterEach
    public void teardown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }

        if (client != null) {
            client.close();
        }
        server = null;
    }

    @Test
    public void connectionReceiveServerErrorStatusShouldReuseConnection() throws InterruptedException {
        server.return500OnFirstRequest = true;
        server.closeConnection = false;

        HttpTestUtils.sendGetRequest(server.port(), client).join();
        // The request-complete-future does not await the channel-release-future
        // Wait a small amount to allow the channel release to complete
        Thread.sleep(100);
        HttpTestUtils.sendGetRequest(server.port(), client).join();
        assertThat(server.channels.size()).isEqualTo(1);
    }

    @Test
    public void connectionReceiveOkStatusShouldReuseConnection() throws Exception {
        server.return500OnFirstRequest = false;
        server.closeConnection = false;

        HttpTestUtils.sendGetRequest(server.port(), client).join();

        // The request-complete-future does not await the channel-release-future
        // Wait a small amount to allow the channel release to complete
        Thread.sleep(100);
        
        HttpTestUtils.sendGetRequest(server.port(), client).join();

        assertThat(server.channels.size()).isEqualTo(1);
    }

    @Test
    public void connectionReceiveCloseHeaderShouldNotReuseConnection() throws InterruptedException {
        server.return500OnFirstRequest = false;
        server.closeConnection = true;

        HttpTestUtils.sendGetRequest(server.port(), client).join();
        Thread.sleep(1000);

        HttpTestUtils.sendGetRequest(server.port(), client).join();
        assertThat(server.channels.size()).isEqualTo(2);
    }

    @Test
    public void headRequestResponsesHaveNoPayload() {
        byte[] responseData = HttpTestUtils.sendHeadRequest(server.port(), client).join();

        // The SDK core differentiates between NO data and ZERO bytes of data. Core expects it to be NO data, not ZERO bytes of
        // data for head requests.
        assertThat(responseData).isNull();
    }

    @Test
    public void naughtyHeaderCharactersDoNotGetToServer() {
        String naughtyHeader = "foo\r\nbar";
        assertThatThrownBy(() -> HttpTestUtils.sendRequest(client,
                                                           SdkHttpFullRequest.builder()
                                                                             .uri(URI.create("https://localhost:" + server.port()))
                                                                             .method(SdkHttpMethod.POST)
                                                                             .appendHeader("h", naughtyHeader)
                                                                             .build())
                                              .join())
            .hasCauseInstanceOf(Exception.class);
    }

    @Test
    public void connectionsArePooledByHostAndPort() throws InterruptedException {
        HttpTestUtils.sendRequest(client,
                                  SdkHttpFullRequest.builder()
                                                    .uri(URI.create("https://127.0.0.1:" + server.port() + "/foo?foo"))
                                                    .method(SdkHttpMethod.GET)
                                                    .build())
                     .join();

        Thread.sleep(1_000);

        HttpTestUtils.sendRequest(client,
                                  SdkHttpFullRequest.builder()
                                                    .uri(URI.create("https://127.0.0.1:" + server.port() + "/bar?bar"))
                                                    .method(SdkHttpMethod.GET)
                                                    .build())
            .join();

        assertThat(server.channels.size()).isEqualTo(1);

    }

    @Test
    public void doesNotRetryOn429StatusCode() throws InterruptedException {
        server.return429OnFirstRequest = true;
        server.closeConnection = false;

        HttpTestUtils.sendGetRequest(server.port(), client).join();
        // Wait to ensure no retries happen
        Thread.sleep(100);

        // Verify only one request was made (no retries)
        assertThat(server.requestCount).isEqualTo(1);

        // Send second request to verify connection reuse works after 429
        HttpTestUtils.sendGetRequest(server.port(), client).join();

        // Verify connection was reused and total of 2 requests
        assertThat(server.channels.size()).isEqualTo(1);
        assertThat(server.requestCount).isEqualTo(2);
    }

    private static class Server extends ChannelInitializer<Channel> {
        private static final byte[] CONTENT = "helloworld".getBytes(StandardCharsets.UTF_8);
        private ServerBootstrap bootstrap;
        private ServerSocketChannel serverSock;
        private List<Channel> channels = new ArrayList<>();
        private final NioEventLoopGroup group = new NioEventLoopGroup();
        private SslContext sslCtx;
        private boolean return500OnFirstRequest;
        private boolean closeConnection;
        private boolean return429OnFirstRequest;
        private volatile int requestCount = 0;


        public void init() throws Exception {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

            bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .group(group)
                .childHandler(this);

            serverSock = (ServerSocketChannel) bootstrap.bind(0).sync().channel();
        }

        public void shutdown() throws InterruptedException {
            group.shutdownGracefully().await();
            serverSock.close();
        }

        public int port() {
            return serverSock.localAddress().getPort();
        }

        @Override
        protected void initChannel(Channel ch) {
            channels.add(ch);
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new BehaviorTestChannelHandler());
        }

        private class BehaviorTestChannelHandler extends ChannelDuplexHandler {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof HttpRequest) {
                    requestCount++;

                    HttpResponseStatus status;
                    if (ctx.channel().equals(channels.get(0)) && return500OnFirstRequest) {
                        status = INTERNAL_SERVER_ERROR;
                    } else if (ctx.channel().equals(channels.get(0)) && return429OnFirstRequest) {
                        status = HttpResponseStatus.TOO_MANY_REQUESTS;
                        return429OnFirstRequest = false; // Reset after first use
                    } else {
                        status = OK;
                    }

                    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                                                                            Unpooled.wrappedBuffer(CONTENT));

                    response.headers()
                            .set(CONTENT_TYPE, TEXT_PLAIN)
                            .setInt(CONTENT_LENGTH, response.content().readableBytes());

                    if (closeConnection) {
                        response.headers().set(CONNECTION, CLOSE);
                    }

                    ctx.writeAndFlush(response);
                }
            }
        }
    }
}

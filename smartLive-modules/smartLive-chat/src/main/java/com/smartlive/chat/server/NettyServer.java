package com.smartlive.chat.server;

import com.smartlive.chat.handle.NettyChatHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NettyServer implements CommandLineRunner {

    // 注入刚才写的 Handler
    @Autowired
    private NettyChatHandler nettyChatHandler;

    @Override
    public void run(String... args) {
        // 启动一个新线程去跑 Netty，否则会阻塞 SpringBoot 主线程
        new Thread(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                // HTTP 编解码
                                pipeline.addLast(new HttpServerCodec());
                                // 块写入
                                pipeline.addLast(new ChunkedWriteHandler());
                                // HTTP 消息聚合 (防止半包)
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                // 处理 WebSocket 握手、心跳 (路径要和前端一致)
                                pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                                // 心跳时间间间隔，读空闲时间，写空闲时间，时间单位
                                pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                                // ★★★ 我们的业务处理器 ★★★
                                pipeline.addLast(nettyChatHandler);
                            }
                        });

                // 监听 8888 端口 (不要和 SpringBoot 的 8080 冲突)
                ChannelFuture f = b.bind(8888).sync();
                log.info("🚀 Netty WebSocket 服务器启动成功，端口: 8888");
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty 启动失败", e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }).start();
    }
}
package com.smartlive.im.server;

import com.smartlive.im.handler.NettyChatHandler;
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

    @Autowired
    private NettyChatHandler nettyChatHandler;

    @Override
    public void run(String... args) {
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
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new ChunkedWriteHandler());
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                                pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                                pipeline.addLast(nettyChatHandler);
                            }
                        });

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

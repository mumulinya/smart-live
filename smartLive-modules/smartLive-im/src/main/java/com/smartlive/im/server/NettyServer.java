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

/**
 * Netty WebSocket 服务器启动类
 * 实现 CommandLineRunner 接口，在 Spring Boot 应用启动后自动在独立线程中开启 Netty 服务。
 * 负责配置网络传输通道与处理器流水线（Pipeline）。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Component
public class NettyServer implements CommandLineRunner {

    @Autowired
    private NettyChatHandler nettyChatHandler;

    /**
     * 服务启动入口
     * 使用双线程组模型 (Reactor 模型) 监听并处理客户端 WebSocket 连接请求。
     */
    @Override
    public void run(String... args) {
        new Thread(() -> {
            // bossGroup 负责接收客户端的连接请求
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            // workerGroup 负责具体的读写 IO 各种业务逻辑处理
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                // HTTP 编解码支持
                                pipeline.addLast(new HttpServerCodec());
                                // 支持大数据流写入（如文件传输）
                                pipeline.addLast(new ChunkedWriteHandler());
                                // 将 HTTP 消息聚合为完整对象，限制内容长度为 64KB
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                // 核心 WebSocket 协议支持，暴露路径为 /ws
                                pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                                // 读空闲检测，60秒未收到数据则触发 IdleStateEvent，用于心跳检测
                                pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                                // 业务逻辑处理器
                                pipeline.addLast(nettyChatHandler);
                            }
                        });

                // 绑定并同步监听 8888 端口
                ChannelFuture f = b.bind(8888).sync();
                log.info("🚀 Netty WebSocket 服务器启动成功，端口: 8888");
                // 阻塞直到服务器通道关闭
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty 启动失败", e);
            } finally {
                // 优雅停机，释放线程资源
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }).start();
    }
}

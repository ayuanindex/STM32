package com.realmax.stm32.tcp;

import android.util.Log;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyLinkUtil {
    private static final String TAG = "NettyLinkUtil";
    private String HOST;
    private int PORT;


    public NettyLinkUtil(String HOST, int PORT) {
        this.HOST = HOST;
        this.PORT = PORT;
    }

    public void start(Callback status, SimpleChannelInboundHandler<ByteBuf> nettyHandler) throws Exception {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup) // 注册线程池
                    .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
                    .remoteAddress(new InetSocketAddress(this.HOST, this.PORT)) // 绑定连接端口和host信息
                    .handler(new ChannelInitializer<SocketChannel>() { // 绑定连接初始化器
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            Log.d(TAG, "initChannel: connected...");
                            ch.pipeline().addLast(nettyHandler);
                        }
                    });
            Log.d(TAG, "start: created..");


            ChannelFuture cf = b.connect().sync(); // 异步连接服务器
            Log.d(TAG, "start: connected..."); // 连接完成
            status.success(eventLoopGroup);

            cf.channel().closeFuture().sync(); // 异步等待关闭连接channel
            Log.d(TAG, "start: closed.."); // 关闭完成
            status.error();
        } finally {
            eventLoopGroup.shutdownGracefully().sync(); // 释放线程池资源
        }
    }

    public interface Callback {
        void success(EventLoopGroup eventLoopGroup);

        void error();
    }
}

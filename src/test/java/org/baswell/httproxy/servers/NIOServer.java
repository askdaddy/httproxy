package org.baswell.httproxy.servers;

import org.baswell.httproxy.*;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class NIOServer
{
  public static void main(String[] args) throws Exception
  {
    ServerSocketChannelAcceptLoop acceptLoop = new ServerSocketChannelAcceptLoop(new SimpleNIOProxyDirector("localhost", 8080));
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(9090));
    acceptLoop.start(serverSocketChannel);
  }
}

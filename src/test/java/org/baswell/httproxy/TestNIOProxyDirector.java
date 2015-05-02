package org.baswell.httproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TestNIOProxyDirector extends TestProxyDirector implements NIOProxyDirector
{
  public String serverHost = "localhost";

  public int serverPort = 9096;

  public int maxWriteAttempts = 5;

  @Override
  public int getMaxWriteAttempts()
  {
    return maxWriteAttempts;
  }

  @Override
  public SocketChannel connectToProxiedHost(ProxiedRequest request) throws IOException
  {
    return SocketChannel.open(new InetSocketAddress(serverHost, serverPort));
  }
}

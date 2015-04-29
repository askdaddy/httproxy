package org.baswell.httproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class SimpleNIOProxyDirector extends SimpleProxyDirector implements NIOProxyDirector
{
  public SimpleNIOProxyDirector(String proxiedHost, int proxiedPort)
  {
    super(proxiedHost, proxiedPort);
  }

  @Override
  public int getMaxWriteAttempts()
  {
    return 5;
  }

  @Override
  public SocketChannel connectToProxiedHost(ProxiedRequest request) throws IOException
  {
    return SocketChannel.open(new InetSocketAddress(proxiedHost, proxiedPort));
  }

}

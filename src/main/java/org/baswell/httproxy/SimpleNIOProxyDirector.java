package org.baswell.httproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * A simple NIOProxyDirector that proxies to a single server and prints out proxy events.
 */
public class SimpleNIOProxyDirector extends SimpleProxyDirector implements NIOProxyDirector
{
  /**
   * @see #getMaxWriteAttempts()
   */
  public int maxWriteAttempts = 5;

  public SimpleNIOProxyDirector(String proxiedHost, int proxiedPort)
  {
    super(proxiedHost, proxiedPort);
  }

  @Override
  public int getMaxWriteAttempts()
  {
    return maxWriteAttempts;
  }

  @Override
  public SocketChannel connectToProxiedHost(ProxiedRequest request) throws IOException
  {
    return SocketChannel.open(new InetSocketAddress(proxiedHost, proxiedPort));
  }

}

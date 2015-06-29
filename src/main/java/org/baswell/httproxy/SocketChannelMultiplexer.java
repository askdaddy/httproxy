package org.baswell.httproxy;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

class SocketChannelMultiplexer extends ConnectionMultiplexer<SocketChannel>
{
  private final ExecutorService executorService;

  private final ProxyLogger logger;

  SocketChannelMultiplexer(ExecutorService executorService, ProxyLogger logger)
  {
    this.executorService = executorService;
    this.logger = logger;
  }

  @Override
  protected SocketChannel connect(ConnectionParameters connectionParameters) throws IOException
  {
    InetSocketAddress address = new InetSocketAddress(connectionParameters.ipOrHost, connectionParameters.port);
    SocketChannel socketChannel = SocketChannel.open(address);
    socketChannel.configureBlocking(false);

    if (connectionParameters.ssl)
    {
      SSLEngine sslEngine = connectionParameters.sslContext.createSSLEngine(address.getHostName(), address.getPort());
      sslEngine.setUseClientMode(true);
      socketChannel = new SSLSocketChannel(socketChannel, sslEngine, executorService, logger);
    }

    return socketChannel;
  }

  @Override
  protected void closeQuitely(SocketChannel socketChannel)
  {
    try
    {
      if (socketChannel.isOpen())
      {
        socketChannel.close();
      }
    }
    catch (IOException e)
    {}
  }
}

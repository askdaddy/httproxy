package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

class ProxiedRequestChannel extends ProxiedMessageChannel
{
  ProxiedRequestChannel(ProxiedExchangeChannel proxiedChannel, SocketChannel readChannel, NIOProxyDirector proxyDirector)
  {
    super(true, proxiedChannel, readChannel, proxyDirector);
  }

  @Override
  protected ProxiedRequest proxiedRequest()
  {
    return this;
  }

  @Override
  protected ProxiedResponse proxiedResponse()
  {
    return proxiedChannel.responseChannel;
  }

  @Override
  protected void onHeadersProcessed() throws IOException
  {
    super.onHeadersProcessed();
    if (writeChannel == null)
    {
      writeChannel = proxiedChannel.connectProxiedServer();
    }
  }
}

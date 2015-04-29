package org.baswell.httproxy;

import java.nio.channels.SocketChannel;

class ProxiedResponseChannel extends ProxiedMessageChannel
{
  ProxiedResponseChannel(ProxiedExchangeChannel proxiedChannel, SocketChannel readChannel, SocketChannel writeChannel, NIOProxyDirector proxyDirector)
  {
    super(false, proxiedChannel, readChannel, writeChannel, proxyDirector);
  }

  @Override
  protected ProxiedRequest proxiedRequest()
  {
    return proxiedChannel.requestChannel;
  }

  @Override
  protected ProxiedResponse proxiedResponse()
  {
    return this;
  }
}

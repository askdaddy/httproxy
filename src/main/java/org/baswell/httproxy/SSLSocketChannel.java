package org.baswell.httproxy;

import java.nio.channels.SocketChannel;

public interface SSLSocketChannel
{
  int getApplicationBufferSize();

  SocketChannel getUnderlyingSocketChannel();
}

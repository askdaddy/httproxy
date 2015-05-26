package org.baswell.httproxy;

import java.nio.channels.SocketChannel;

public interface WrappedSocketChannel
{
  SocketChannel unwrap();
}

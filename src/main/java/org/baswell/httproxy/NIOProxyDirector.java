package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * A ProxyDirector for non-blocking IO. All implementations of this interface <strong>must</strong> be thread-safe.
 */
public interface NIOProxyDirector extends ProxyDirector
{
  /**
   *
   * @return The number of continuous attempts to write all buffered bytes to a SocketChannel's write buffer. If all bytes
   * cannot be written the remaining bytes in the buffer will be held in memory until a write ready event is triggered for the SocketChannel.
   */
  int getMaxWriteAttempts();

  /**
   * Create a socket connection for the given proxied request.
   *
   * @param request The proxied request.
   * @return The server connection the given request will be proxied to.
   * @throws IOException If the server connection
   */
  SocketChannel connectToProxiedHost(ProxiedRequest request) throws IOException;
}

package org.baswell.httproxy;

import java.io.IOException;
import java.net.Socket;


/**
 * A ProxyDirector for blocking IO. All implementations of this interface <strong>must</strong> be thread-safe.
 */
public interface IOProxyDirector extends ProxyDirector
{
  /**
   *
   * @return The number of seconds to sleep when a Socket's inputstream is still open but no content is available.
   */
  int getSleepSecondsOnReadWait();

  /**
   * Create a socket connection for the given proxied request.
   *
   * @param request The proxied request.
   * @return The server connection the given request will be proxied to.
   * @throws IOException If the server connection
   */
  Socket connectToProxiedHost(ProxiedRequest request) throws IOException;
}

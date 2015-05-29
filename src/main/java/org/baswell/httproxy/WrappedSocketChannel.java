package org.baswell.httproxy;

import java.nio.channels.SocketChannel;

/**
 * External implementations of an SSL supported SocketChannel should implement this interface so that the real {@link SocketChannel} is
 * used to register with the {@link java.nio.channels.Selector}.
 *
 */
public interface WrappedSocketChannel
{
  /**
   *
   * @return The real SocketChannel this implementation wraps.
   */
  SocketChannel getWrappedSocketChannel();
}

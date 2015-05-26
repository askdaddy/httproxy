/*
 * Copyright 2015 Corey Baswell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * A ProxyDirector for non-blocking IO. All implementations of this interface <strong>must be thread-safe</strong>.
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
   * Create a socket connection for the given proxied request. If <code>null</code> is a returned a 404 will be
   * returned to the client.
   *
   * @param request The proxied request.
   * @return The server connection the given request will be proxied to.
   * @throws IOException If the server connection cannot be made.
   * @throws EndProxiedRequestException To return the provided HTTP status and message and end the proxied the request.
   */
  SocketChannel connectToProxiedHost(ProxiedRequest request) throws IOException, EndProxiedRequestException;
}

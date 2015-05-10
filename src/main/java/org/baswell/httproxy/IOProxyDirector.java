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
import java.net.Socket;


/**
 * A ProxyDirector for blocking IO. All implementations of this interface <strong>must be thread-safe</strong>.
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

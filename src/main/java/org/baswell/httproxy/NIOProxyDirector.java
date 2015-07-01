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

import java.util.concurrent.ExecutorService;

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
   * @return The thread pool used to execute long running SSL operations (like CA verification). Can be null if not using SSL connections.
   */
  ExecutorService getSSLThreadPool();
}

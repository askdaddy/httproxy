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

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

/**
 * A ProxyDirector for blocking IO. All implementations of this interface <strong>must be thread-safe</strong>.
 */
public interface IOProxyDirector extends ProxyDirector
{
  /**
   * The maximum milliseconds a read operation on a socket will block (for either client or server) before a SocketTimeoutException
   * is thrown.
   *
   * @return The socket read timeout in milliseconds or <code>null</code> if not timeout is desired.
   */
  Integer getSocketReadTimeoutMilliseconds();

  /**
   *
   * @return The number of seconds to sleep when a Socket's inputstream is still open but no content is available.
   */
  int getSleepSecondsOnReadWait();

  /**
   * @return The thread pool used to execute blocking IO requests & responses.
   */
  ExecutorService getIOThreadPool();

  /**
   *
   * @return Use a background thread to expire exchanges when the Keep-Alive timeout from the server expires.
   */
  boolean useKeepAliveReaper();

  /**
   *
   * @return The maximum seconds the keep alive background thread will sleep between checks.
   */
  int getKeepAliveSleepSeconds();

  /**
   * After the HTTP request header is sent from the client the content of the request (if present) can be modified here. <code>null</code> can be returned here to send to the
   * proxied server exactly what the client sent in the content of the request.
   *
   * @param httpRequest The HTTP request from the client.
   * @return The modifier of this request's content or <code>null</code> to use the raw client input stream.
   */
  RequestContentModifier getRequestModifier(HttpRequest httpRequest);

  /**
   * After the HTTP response header is sent back to the client the content response sent back can be modified here. <code>null</code> can be returned here to use the raw client output stream.
   *
   * @param httpRequest The HTTP request that produced the given response.
   * @param httpResponse The HTTP response.
   * @return The modifier of the response's content or <code>null</code> to use the raw client output stream.
   */
  ResponseContentModifier getResponseModifier(HttpRequest httpRequest, HttpResponse httpResponse);
}

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

/**
 * Base interface for receiving proxy events and manipulating request & response headers. All implementations of this
 * interface <strong>must be thread-safe</strong>.
 */
public interface ProxyDirector
{
  /**
   *
   * @return The size in bytes of the read buffer.
   */
  int getBufferSize();

  /**
   * The socket connection parameters for the given proxied httpRequest. If <code>null</code> is a returned a 404 will be
   * returned to the client.
   *
   * The proxied httpRequest can
   *
   * @param httpRequest The proxied httpRequest.
   * @return The server connection the given httpRequest will be proxied to.
   * @throws EndProxiedRequestException To return the provided HTTP status and message and end the proxied the httpRequest.
   */
  ConnectionParameters onRequest(HttpRequest httpRequest) throws EndProxiedRequestException;

  void onResponse(HttpResponse response, ConnectionParameters connectionParameters);

  /**
   * Called when an httpRequest-response exchange has been completed.
   * @param httpRequest The proxied httpRequest.
   * @param response The proxied response.
   */
  void onExchangeComplete(HttpRequest httpRequest, HttpResponse response);

  /**
   * Called when a httpRequest could not be correctly parsed.
   *
   * @param httpRequest The proxied httpRequest.
   * @param errorDescription A description of the protocol error.
   */
  void onRequestHttpProtocolError(HttpRequest httpRequest, String errorDescription);

  /**
   * Called when a response could not be correctly parsed.
   *
   * @param httpRequest The proxied httpRequest.
   * @param response The proxied response.
   * @param errorDescription A description of the protocol error.
   */
  void onResponseHttpProtocolError(HttpRequest httpRequest, HttpResponse response, String errorDescription);

  /**
   * Called when the client connection was closed before the httpRequest was fully read or the response was returned.
   *
   * @param httpRequest The proxied httpRequest.
   * @param e The IO exception that signaled the close.
   */
  void onPrematureRequestClosed(HttpRequest httpRequest, IOException e);

  /**
   * Called when the server connection was closed before a response was retrieved.
   *
   * @param httpRequest The proxied httpRequest.
   * @param response The proxied response.
   * @param e The IO exception that signaled the close.
   */
  void onPrematureResponseClosed(HttpRequest httpRequest, HttpResponse response, ConnectionParameters connectionParameters, IOException e);

  /**
   * The logger used by the HttProxy runtime.
   *
   * @return A logger or {@code null} to disable logging.
   */
  ProxyLogger getLogger();
}

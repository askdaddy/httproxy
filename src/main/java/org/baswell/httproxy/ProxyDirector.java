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
   * Before the given HTTP request is sent to the server. The HTTP request (status line and headers) can be modified here55.
   *
   * @param httpRequest The HTTP request.
   * @return The server connection the given httpRequest will be proxied to. If <code>null</code> is a returned a 404 will be
   * returned to the client.
   * @throws EndProxiedRequestException To return the provided HTTP status and message and end the proxied the httpRequest.
   */
  ConnectionParameters onRequestStart(HttpRequest httpRequest) throws EndProxiedRequestException;

  /**
   * Notification when a connection was unable to be established.
   *
   * @param httpRequest The HTTP request this connection failed on.
   * @param connectionParameters The connection parameters previously return from {@link #onRequestStart(HttpRequest)}.
   */
  void onConnectionFailed(HttpRequest httpRequest, ConnectionParameters connectionParameters, IOException e);

  /**
   * After the given HTTP request has been sent to the server (including body). Modifying the given HTTP request here will
   * have no impact as the request has already been sent.
   *
   * @param httpRequest The HTTP request sent to the server.
   * @param connectionParameters The connection parameters used for the server.
   */
  void onRequestEnd(HttpRequest httpRequest, ConnectionParameters connectionParameters);

  /**
   * Before the given HTTP response is sent back to the client. The HTTP response (status line and headers) can be modified here
   * before it is sent back to the client.
   *
   * @param httpRequest The HTTP request that produced the given response.
   * @param httpResponse The HTTP response.
   */
  void onResponseStart(HttpRequest httpRequest, HttpResponse httpResponse);

  /**
   * After the given HTTP response has been sent to the client (including body). Modifying the given HTTP response here will
   * have no impact as the response has already been sent.
   *
   * @param httpRequest The HTTP request that produced the given response.
   * @param httpResponse The HTTP response.
   */
  void onResponseEnd(HttpRequest httpRequest, HttpResponse httpResponse);

  /**
   * Called when a HTTP request could not be correctly parsed.
   *
   * @param httpRequest The HTTP request. May be null.
   * @param errorDescription A description of the protocol error.
   */
  void onRequestHttpProtocolError(HttpRequest httpRequest, String errorDescription);

  /**
   * Called when a HTTP response could not be correctly parsed.
   *
   * @param httpRequest The HTTP request that produced the given response.
   * @param httpResponse The HTTP response. May be null.
   * @param errorDescription A description of the protocol error.
   */
  void onResponseHttpProtocolError(HttpRequest httpRequest, HttpResponse httpResponse, String errorDescription);

  /**
   * Called when the client connection was closed before the HTTP request was fully read or the response was returned.
   *
   * @param httpRequest The HTTP request. May be null.
   * @param e The IO exception that signaled the close.
   */
  void onPrematureRequestClosed(HttpRequest httpRequest, IOException e);

  /**
   * Called when the server connection was closed before a response was retrieved.
   *
   * @param httpRequest The HTTP request that produced the given response.
   * @param connectionParameters The connection parameters to the backend server that shutdown the request.
   * @param e The IO exception that signaled the close.
   */
  void onPrematureResponseClosed(HttpRequest httpRequest, ConnectionParameters connectionParameters, IOException e);

  /**
   * The logger used by the HttProxy runtime.
   *
   * @return A logger or {@code null} to disable logging.
   */
  ProxyLogger getLogger();
}

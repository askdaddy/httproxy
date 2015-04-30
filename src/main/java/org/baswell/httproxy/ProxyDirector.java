package org.baswell.httproxy;

import java.io.IOException;
import java.util.List;

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
   * Called for each header processed in a request. If a different value for this header needs to be sent to the server
   * return a non-null value. If {@code null} is returned the header will be sent to the server unaltered.
   *
   * @param headerName The name of the request header.
   * @param headerValue The value of the request header.
   * @param request The proxied request.
   * @return A non-null header value to alter what is sent to the server or {@code null} to send the header unaltered.
   */
  String siftRequestHeader(String headerName, String headerValue, ProxiedRequest request);

  /**
   *
   * @param request The proxied request.
   * @return A list of additional of headers to send to the server or {@code null} to not add any headers.
   */
  List<Header> addRequestHeaders(ProxiedRequest request);

  /**
   * Called for each header processed in a response. If a different value for this header needs to be sent to the client
   * return a non-null value. If {@code null} is returned the header will be sent to the client unaltered.
   *
   * @param headerName The name of the request header.
   * @param headerValue The value of the request header.
   * @param request The proxied request.
   * @param response The proxied response.
   * @return A non-null header value to alter what is sent to the server or {@code null} to send the header unaltered.
   */
  String siftResponseHeader(String headerName, String headerValue, ProxiedRequest request, ProxiedResponse response);

  /**
   *
   * @param request The proxied request.
   * @param response The proxied response.
   * @return A list of additional of headers to send to the client or {@code null} to not add any headers.
   */
  List<Header> addResponseHeaders(ProxiedRequest request, ProxiedResponse response);

  /**
   * Called when an request-response exchange has been completed.
   * @param request The proxied request.
   * @param response The proxied response.
   */
  void onExchangeComplete(ProxiedRequest request, ProxiedResponse response);

  /**
   * Called when a request could not be correctly parsed.
   *
   * @param request The proxied request.
   * @param errorDescription A description of the protocol error.
   */
  void onRequestHttpProtocolError(ProxiedRequest request, String errorDescription);

  /**
   * Called when a response could not be correctly parsed.
   *
   * @param request The proxied request.
   * @param response The proxied response.
   * @param errorDescription A description of the protocol error.
   */
  void onResponseHttpProtocolError(ProxiedRequest request, ProxiedResponse response, String errorDescription);

  /**
   * Called when the client connection was closed before the request was fully read or the response was returned.
   *
   * @param request The proxied request.
   * @param e The IO exception that signaled the close.
   */
  void onPrematureRequestClosed(ProxiedRequest request, IOException e);

  /**
   * Called when the server connection was closed before a response was retrieved.
   *
   * @param request The proxied request.
   * @param response The proxied response.
   * @param e The IO exception that signaled the close.
   */
  void onPrematureResponseClosed(ProxiedRequest request, ProxiedResponse response, IOException e);
}

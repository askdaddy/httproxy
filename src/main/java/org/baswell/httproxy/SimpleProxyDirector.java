package org.baswell.httproxy;

import java.io.IOException;
import java.util.List;

/**
 * A simple ProxyDirector that proxies to a single server and prints out proxy events.
 */
abstract public class SimpleProxyDirector implements ProxyDirector
{
  /**
   * @see #getBufferSize()
   */
  public int bufferSize = 1024 * 16;

  protected final String proxiedHost;

  protected final int proxiedPort;

  protected SimpleProxyDirector(String proxiedHost, int proxiedPort)
  {
    this.proxiedHost = proxiedHost;
    this.proxiedPort = proxiedPort;
  }

  @Override
  public int getBufferSize()
  {
    return bufferSize;
  }

  @Override
  public String siftRequestHeader(String headerName, String headerValue, ProxiedRequest request)
  {
    return null;
  }

  @Override
  public List<Header> addRequestHeaders(ProxiedRequest request)
  {
    return null;
  }

  @Override
  public String siftResponseHeader(String headerName, String headerValue, ProxiedRequest request, ProxiedResponse response)
  {
    return null;
  }

  @Override
  public List<Header> addResponseHeaders(ProxiedRequest request, ProxiedResponse response)
  {
    return null;
  }

  @Override
  public void onExchangeComplete(ProxiedRequest request, ProxiedResponse response)
  {
    System.out.println(request.path() + " - " + (request.startedAt() - response.endedAt()));
  }

  @Override
  public void onRequestHttpProtocolError(ProxiedRequest request, String errorDescription)
  {
    System.err.println("Request HTTP protocol error: " + errorDescription + " for request: " + request.path());
  }

  @Override
  public void onResponseHttpProtocolError(ProxiedRequest request, ProxiedResponse response, String errorDescription)
  {
    System.err.println("Response HTTP protocol error: " + errorDescription + " for request: " + request.path());
  }

  @Override
  public void onPrematureRequestClosed(ProxiedRequest request, IOException e)
  {
    System.err.println("Premature request closed for request: " + request.path());
  }

  @Override
  public void onPrematureResponseClosed(ProxiedRequest request, ProxiedResponse response, IOException e)
  {
    System.err.println("Premature response closed for request: " + request.path());
  }
}

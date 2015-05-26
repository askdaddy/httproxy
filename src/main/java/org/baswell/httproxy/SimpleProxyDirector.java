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
    if (headerName.equalsIgnoreCase("Location") && headerValue.startsWith("https://"))
    {
      return "http://" + headerValue.substring("https://".length(), headerValue.length());
    }
    else
    {
      return null;
    }
  }

  @Override
  public List<Header> addResponseHeaders(ProxiedRequest request, ProxiedResponse response)
  {
    return null;
  }

  @Override
  public void onExchangeComplete(ProxiedRequest request, ProxiedResponse response)
  {
    System.out.println(request.path() + " - " + (response.endedAt() - request.startedAt() ));
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

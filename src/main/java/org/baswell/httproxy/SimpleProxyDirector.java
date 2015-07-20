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

import javax.net.ssl.SSLContext;
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

  public int logLevel =  SimpleProxyLogger.INFO_LEVEL;

  protected final String proxiedHost;

  protected final int proxiedPort;

  protected final SSLContext sslContext;

  protected final ConnectionParameters connectionParameters;

  protected SimpleProxyDirector(String proxiedHost, int proxiedPort)
  {
    this(proxiedHost, proxiedPort, null);
  }

  protected SimpleProxyDirector(String proxiedHost, int proxiedPort, SSLContext sslContext)
  {
    this.proxiedHost = proxiedHost;
    this.proxiedPort = proxiedPort;
    this.sslContext = sslContext;

    connectionParameters = new ConnectionParameters(proxiedHost, proxiedPort, sslContext);
  }

  @Override
  public int getBufferSize()
  {
    return bufferSize;
  }

  @Override
  public ConnectionParameters onRequestStart(HttpRequest httpRequest) throws EndProxiedRequestException
  {
    System.out.println("--> " + httpRequest.getStatusLine());
    return connectionParameters;
  }

  @Override
  public void onConnectionFailed(HttpRequest httpRequest, ConnectionParameters connectionParameters, IOException e)
  {
    System.err.println("Connection failed to: " + connectionParameters + " for request: "+ httpRequest.getStatusLine());
    e.printStackTrace();
  }

  @Override
  public void onRequestEnd(HttpRequest httpRequest, ConnectionParameters connectionParameters)
  {}

  @Override
  public void onResponseStart(HttpRequest httpRequest, HttpResponse httpResponse, ConnectionParameters connectionParameters)
  {
    System.out.println(" <-- " + httpResponse.getStatusLine());
  }

  @Override
  public void onResponseEnd(HttpRequest httpRequest, HttpResponse response, ConnectionParameters connectionParameters)
  {}

  @Override
  public void onRequestHttpProtocolError(HttpRequest httpRequest, String errorDescription)
  {
    System.err.println("Request protocol error: " + errorDescription);
  }

  @Override
  public void onResponseHttpProtocolError(HttpRequest httpRequest, HttpResponse response, ConnectionParameters connectionParameters, String errorDescription)
  {
    System.err.println("Response protocol error: " + errorDescription);
  }

  @Override
  public void onPrematureRequestClosed(HttpRequest httpRequest, IOException e)
  {
    System.err.println("Request premature closed: " + e.getMessage());
    e.printStackTrace();
  }

  @Override
  public void onPrematureResponseClosed(HttpRequest httpRequest, HttpResponse response, ConnectionParameters connectionParameters, IOException e)
  {
    System.err.println("Response premature closed: " + e.getMessage());
    e.printStackTrace();
  }

  @Override
  public ProxyLogger getLogger()
  {
    return new SimpleProxyLogger(logLevel);
  }
}

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
import java.security.NoSuchAlgorithmException;

/**
 * Describes the connection parameters to a proxied server.
 *
 * @see ProxyDirector#onRequestStart(HttpRequest)
 */
public class ConnectionParameters
{
  public final boolean ssl;

  public final String ipOrHost;

  public final int port;

  public final SSLContext sslContext;

  public Object attachement;

  /**
   *
   * @param ipOrHost The IP address or hostname of the server.
   * @param port The HTTP port to connect on.
   */
  public ConnectionParameters(String ipOrHost, int port)
  {
    this(ipOrHost, port, null);
  }

  /**
   *
   * @param ssl Is this connection SSL? If so the default SSL context ({@link SSLContext#getDefault()} will be used.
   * @param ipOrHost The IP address or hostname of the server.
   * @param port The HTTP port to connect on.
   */
  public ConnectionParameters(boolean ssl, String ipOrHost, int port)
  {
    this(ipOrHost, port, ssl ? defaultSSLContext() : null);
  }

  /**
   *
   * @param ipOrHost The IP address or hostname of the server.
   * @param port The HTTP port to connect on.
   * @param sslContext The SSL context to use for SSL connections (or null if this is a non-secure connection).
   */
  public ConnectionParameters(String ipOrHost, int port, SSLContext sslContext)
  {
    this.ipOrHost = ipOrHost;
    this.port = port;
    this.sslContext = sslContext;

    ssl = sslContext != null;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConnectionParameters that = (ConnectionParameters) o;

    if (ssl != that.ssl) return false;
    if (port != that.port) return false;
    return !(ipOrHost != null ? !ipOrHost.equals(that.ipOrHost) : that.ipOrHost != null);
  }

  @Override
  public int hashCode()
  {
    int result = (ssl ? 1 : 0);
    result = 31 * result + (ipOrHost != null ? ipOrHost.hashCode() : 0);
    result = 31 * result + port;
    return result;
  }

  private static SSLContext defaultSSLContext()
  {
    try
    {
      return SSLContext.getDefault();
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new RuntimeException(e);
    }
  }
}

package org.baswell.httproxy;

import javax.net.ssl.SSLContext;

public class ConnectionParameters
{
  public final boolean ssl;

  public final String ipOrHost;

  public final int port;

  public final SSLContext sslContext;

  public ConnectionParameters(String ipOrHost, int port)
  {
    this(ipOrHost, port, null);
  }

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
}

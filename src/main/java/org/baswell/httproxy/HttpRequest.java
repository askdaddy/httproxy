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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A proxied HTTP request. The request sent back to the client can be modified in {@link ProxyDirector#onRequestStart(HttpRequest)}.
 */
public class HttpRequest extends HttpMessage
{
  /**
   * The client IP address this request came from.
   */
  public final String clientIp;

  /**
   * Is this request the first in the exchange (multiple requests & responses can be processed over the same connection).
   */
  public final boolean firstInExchange;

  /**
   * Was the request received over a SSL connection.
   */
  public final boolean overSSL;

  /**
   * The HTTP method (ex. GET, POST, PUT)
   */
  public String method;

  /**
   * The HTTP path requested.
   */
  public String path;

  /**
   * The HTTP version of this request.
   */
  public String version;

  /**
   *
   * @param clientIp The client IP address this request came from.
   * @param firstInExchange Is this request the first in the exchange.
   * @param overSSL Was the request received over a SSL connection.
   * @param requestLine The HTTP request line (ex. <i>GET /path/to/file/index.html HTTP/1.0</i>
   */
  public HttpRequest(String clientIp, boolean firstInExchange, boolean overSSL, String requestLine)
  {
    this.clientIp = clientIp;
    this.firstInExchange = firstInExchange;
    this.overSSL = overSSL;

    String[] values = new String(requestLine).trim().split(" ");
    for (int i = 0; i < values.length; i++)
    {
      String value = values[i].trim();

      if (!value.isEmpty())
      {
        if (method == null)
        {
          method = value;
        }
        else if (path == null)
        {
          path = value;
        }
        else
        {
          version = value;
          break;
        }
      }
    }
  }

  /**
   *
   * @param clientIp The client IP address this request came from.
   * @param firstInExchange Is this request the first in the exchange.
   * @param overSSL Was the request received over a SSL connection.
   * @param method The HTTP method (ex. GET, POST, PUT)
   * @param path The HTTP path requested.
   * @param version The HTTP version of the request.
   */
  public HttpRequest(String clientIp, boolean firstInExchange, boolean overSSL, String method, String path, String version)
  {
    this.version = version;
    this.path = path;
    this.method = method;
    this.overSSL = overSSL;
    this.firstInExchange = firstInExchange;
    this.clientIp = clientIp;
  }

  /**
   *
   * @return Ex. <i>GET /path/to/file/index.html HTTP/1.0</i>.
   */
  @Override
  public String getStatusLine()
  {
    return method + " " + path + " " + version;
  }

  public String getURL()
  {
    return (overSSL ? "https://" : "http://") + getHost() + path;
  }

  /**
   *
   * @return The value of the <i>Host</i> header or <code>null</code> if not Host header is present.
   */
  public String getHost()
  {
    return getHeaderValue("Host");
  }

  /**
   *
   * @return The value of the <i>User-Agent</i> header or <code>null</code> if not User-Agent header is present.
   */
  public String getUserAgent()
  {
    return getHeaderValue("User-Agent");
  }

  /**
   *
   * @return Parse and return all cookies contain in the headers of this request.
   */
  public List<HttpCookie> getCookies()
  {
    List<HttpCookie> cookies = new ArrayList<HttpCookie>();
    for (HttpHeader cookieHeader : getHeaders("Cookie"))
    {
      try
      {
        cookies.addAll(HttpCookie.decodeHeaderValue(cookieHeader.value));
      }
      catch (Exception e)
      {}
    }
    return cookies;
  }

  /**
   * Add a cookies to this request. Should be called from {@link ProxyDirector#onRequestStart(HttpRequest)}.
   * @param cookieToAdded
   */
  public void addCookie(HttpCookie cookieToAdded)
  {
    addCookies(Arrays.asList(cookieToAdded));
  }

  /**
   * Adds cookies to this request. Should be called from {@link ProxyDirector#onRequestStart(HttpRequest)}.
   * @param cookiesToAdded
   */
  public void addCookies(List<HttpCookie> cookiesToAdded)
  {
    List<HttpCookie> cookies = getCookies();
    cookies.addAll(cookiesToAdded);

    setOrAddHeader("Cookie", HttpCookie.encodeHeaderValue(cookies));
  }
}

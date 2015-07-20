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

/**
 * A proxied HTTP response. The response sent back to the client can be modified in {@link ProxyDirector#onResponseStart(HttpRequest, HttpResponse)}.
 */
public class HttpResponse extends HttpMessage
{
  /**
   * Is this response the first in the exchange (multiple requests & responses can be processed over the same connection).
   */
  public final boolean firstInExchange;

  /**
   * Was the response returned over a SSL connection.
   */
  public final boolean overSSL;

  /**
   * The connection this response was returned from.
   */
  public final ConnectionParameters connectionParameters;

  /**
   * The HTTP version of this response.
   */
  public String version;

  /**
   * The status of the response (ex. 200, 404, 500).
   */
  public int statusCode;

  /**
   * The reason phrase of the status code (ex. "OK", "Not Found", "Internal Server Error").
   */
  public String reasonPhrase;

  /**
   *
   * @param firstInExchange Is this response the first in the exchange.
   * @param overSSL Was the response returned over a SSL connection.
   * @param connectionParameters The connection this response was returned from.
   * @param responseLine The HTTP status line ex. <i>HTTP/1.0 200 OK</i>.
   */
  public HttpResponse(boolean firstInExchange, boolean overSSL, ConnectionParameters connectionParameters, String responseLine)
  {
    this.firstInExchange = firstInExchange;
    this.overSSL = overSSL;
    this.connectionParameters = connectionParameters;

    String[] values = new String(responseLine).trim().split(" ");
    for (int i = 0; i < values.length; i++)
    {
      String value = values[i].trim();

      if (!value.isEmpty())
      {
        if (version == null)
        {
          version = value;
        }
        else if (statusCode == 0)
        {
          try
          {
            statusCode = Integer.parseInt(value);
          }
          catch (NumberFormatException e)
          {
            statusCode = -1;
          }
        }
        else
        {
          if (reasonPhrase == null)
          {
            reasonPhrase = value;
          }
          else
          {
            reasonPhrase += " " + value;
          }
          break;
        }
      }
    }
  }

  /**
   *
   * @param firstInExchange Is this response the first in the exchange.
   * @param overSSL Was the response returned over a SSL connection.
   * @param connectionParameters The connection this response was returned from.
   * @param version The HTTP version of this response.
   * @param statusCode The status of the response (ex. 200, 404, 500).
   * @param reasonPhrase The reason phrase of the status code (ex. "OK", "Not Found", "Internal Server Error").
   */
  public HttpResponse(boolean firstInExchange, boolean overSSL, ConnectionParameters connectionParameters, String version, int statusCode, String reasonPhrase)
  {
    this.firstInExchange = firstInExchange;
    this.overSSL = overSSL;
    this.connectionParameters = connectionParameters;
    this.version = version;
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  /**
   * @return ex. <i>HTTP/1.0 200 OK</i>.
   */
  @Override
  public String getStatusLine()
  {
    return version + " " + statusCode + " " + reasonPhrase;
  }

  /**
   * Add a Set-Cookie header to this response. Should be called from {@link ProxyDirector#onResponseStart(HttpRequest, HttpResponse)}
   * for the client to see this additional header.
   *
   * @param cookie The HTTP cookie to add.
   */
  public void setCookie(HttpCookie cookie)
  {
    headers.add(new HttpHeader("Set-Cookie", cookie.toString()));
  }
}

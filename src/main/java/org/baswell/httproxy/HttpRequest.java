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

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

public class HttpRequest extends HttpMessage
{
  public final String clientIp;

  public final boolean firstInExchange;

  public final boolean overSSL;

  public String method;

  public String path;

  public String version;

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

  public String getHost()
  {
    return getHeaderValue("Host");
  }

  public String getUserAgent()
  {
    return getHeaderValue("User-Agent");
  }

  public List<HttpCookie> getCookies()
  {
    List<HttpCookie> cookies = new ArrayList<HttpCookie>();
    for (HttpHeader cookieHeader : getHeaders("Cookie"))
    {
      try
      {
        cookies.addAll(HttpCookie.parse(cookieHeader.value));
      }
      catch (Exception e)
      {}
    }
    return cookies;
  }

  public void addCookie(HttpCookie cookie)
  {
    headers.add(new HttpHeader("Cookie", cookie.toString()));
  }

  @Override
  public String getStatusLine()
  {
    return method + " " + path + " " + version;
  }


}

package org.baswell.httproxy;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

public class HttpRequest extends HttpMessage
{
  public String method;

  public String path;

  public String version;

  public HttpRequest(String requestLine)
  {
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
    for (Header cookieHeader : getHeaders("Cookie"))
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
    headers.add(new Header("Cookie", cookie.toString()));
  }

  @Override
  String getStatusLine()
  {
    return method + " " + path + " " + version;
  }
}

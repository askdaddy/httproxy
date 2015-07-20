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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An HTTP cookie.
 *
 * @see HttpRequest#addCookie(HttpCookie)
 * @see HttpRequest#getCookies()
 * @see HttpResponse#setCookie(HttpCookie)
 */
public class HttpCookie
{
  private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

  private static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

  /**
   * The cookie name.
   */
  public final String name;

  /**
   * The cookie value.
   */
  public final String value;

  /**
   * The domain scope this cookie is relevant for.
   */
  public final String domain;

  /**
   * The path scope this cookie is relevant for.
   */
  public final String path;

  /**
   * Defines a specific date and time for when the browser should delete the cookie. If null the cookie expires with the
   * browser session.
   */
  public final Long expiresAt;

  /**
   * Keep cookie communication limited to encrypted transmission.
   */
  public final boolean secure;

  /**
   * Directs browsers not to expose cookies through channels other than HTTP (and HTTPS) requests.
   */
  public final boolean httpOnly;

  /**
   *
   * @param encodedHeaderValue The encoded cookie from an HTTP header (ex. <i>name=value; Secure</i>).
   */
  public HttpCookie(String encodedHeaderValue)
  {

    String name = null;
    String value = null;
    String domain = null;
    String path = null;
    Long expiresAt = null;
    boolean secure = false;
    boolean httpOnly = false;

    String[] attributes = encodedHeaderValue.split(";");

    if (attributes.length > 0)
    {
      String[] nameValues = attributes[0].trim().split("=");
      if (nameValues.length > 0)
      {
        name = nameValues[0].trim();
      }

      if (nameValues.length > 1)
      {
        value = nameValues[1].trim();
      }
    }

    for (int i = 1; i < attributes.length; i++)
    {
      String attribute = attributes[i].trim();
      String[] attNameValue = attribute.split("=");
      if (attNameValue.length > 1)
      {
        String attName = attNameValue[0].trim();
        String attValue = attNameValue[1].trim();

        if (attName.equalsIgnoreCase("Domain"))
        {
          domain = attValue;
        }
        else if (attName.equalsIgnoreCase("Path"))
        {
          path = attValue;
        }
        else if (attName.equalsIgnoreCase("Expires"))
        {
          try
          {
            String expiresPattern = attValue.contains("-") ? PATTERN_RFC1036 : PATTERN_RFC1123;
            expiresAt = new SimpleDateFormat(expiresPattern).parse(attValue).getTime();
          }
          catch (Exception e)
          {}
        }
      }
      else if (attribute.equalsIgnoreCase("Secure"))
      {
        secure = true;
      }
      else if (attribute.equalsIgnoreCase("HttpOnly"))
      {
        httpOnly = true;
      }
    }

    this.name = name;
    this.value = value;
    this.domain = domain;
    this.path = path;
    this.expiresAt = expiresAt;
    this.secure = secure;
    this.httpOnly = httpOnly;
  }

  /**
   *
   * @param name The cookie name.
   * @param value The cookie value.
   */
  public HttpCookie(String name, String value)
  {
    this(name, value, null, null, null, false, false);
  }

  /**
   *
   * @param name The cookie name.
   * @param value The cookie value.
   * @param domain The domain scope this cookie is relevant for.
   * @param path The path scope this cookie is relevant for.
   */
  public HttpCookie(String name, String value, String domain, String path)
  {
    this(name, value, domain, path, null, false, false);
  }

  /**
   *
   * @param name The cookie name.
   * @param value The cookie value.
   * @param domain The domain scope this cookie is relevant for.
   * @param path The path scope this cookie is relevant for.
   * @param expiresAt Defines a specific date and time for when the browser should delete the cookie. If null the cookie expires with the browser session.
   * @param secure Keep cookie communication limited to encrypted transmission.
   * @param httpOnly The encoded cookie from an HTTP header (ex. <i>name=value; Secure</i>).
   */
  public HttpCookie(String name, String value, String domain, String path, Long expiresAt, boolean secure, boolean httpOnly)
  {
    this.name = name;
    this.value = value;
    this.domain = domain;
    this.path = path;
    this.expiresAt = expiresAt;
    this.secure = secure;
    this.httpOnly = httpOnly;
  }

  @Override
  public String toString()
  {
    String encoded = name + "="+ value;

    if (domain != null)
    {
      encoded += "; Domain=" + domain;
    }

    if (path != null)
    {
      encoded += "; Path=" + path;
    }

    if (expiresAt != null)
    {
      encoded += "; Expires=" + new SimpleDateFormat(PATTERN_RFC1036).format(new Date(expiresAt));
    }

    if (secure)
    {
      encoded += "; Secure";
    }

    if (httpOnly)
    {
      encoded += "; HttpOnly";
    }

    return encoded;
  }

  static List<HttpCookie> parse(String cookieHeaderValue)
  {
    String[] values = cookieHeaderValue.split(";");
    List<HttpCookie> cookies = new ArrayList<HttpCookie>();
    for (String value : values)
    {
      value = value.trim();
      if (!value.isEmpty())
      {
        HttpCookie cookie = new HttpCookie(value);
        if (cookie.name != null && cookie.value != null)
        {
          cookies.add(cookie);
        }
      }
    }
    return cookies;
  }
}

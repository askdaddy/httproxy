package org.baswell.httproxy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HttpCookie
{
  public static List<HttpCookie> parse(String setCookieValue)
  {

  }

  public final String name;

  public final String value;

  public final String domain;

  public final String path;

  public final Long expiresAt;

  public final boolean secure;

  public final boolean httpOnly;

  public HttpCookie(String headerValue)
  {

    String name = null;
    String value = null;
    String domain = null;
    String path = null;
    Long expiresAt = null;
    boolean secure = false;
    boolean httpOnly = false;

    String[] attributes = headerValue.split(";");

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
            expiresAt = new SimpleDateFormat("dd MMM yyyy kk:mm:ss z").parse(attValue).getTime();
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

  public HttpCookie(String name, String value)
  {
    this(name, value, null, null, null, false, false);
  }

  public HttpCookie(String name, String value, String domain, String path)
  {
    this(name, value, domain, path, null, false, false);
  }

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
      encoded += "; Expires=" + new Date(expiresAt).toGMTString();
    }

    if (secure)
    {
      encoded += "; Secure";
    }

    if (httpOnly)
    {
      encoded += "; Secure";
    }

    return encoded;
  }
}

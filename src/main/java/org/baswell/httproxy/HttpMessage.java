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

import gnu.trove.list.array.TByteArrayList;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.baswell.httproxy.Constants.*;

/**
 * Base class for {@link HttpRequest} and {@link HttpResponse}.
 */
abstract public class HttpMessage
{
  /**
   * @return The status line of this message (ex. <i>GET /path/to/file/index.html HTTP/1.0</i>, <i>HTTP/1.0 200 OK</i>).
   */
  abstract public String getStatusLine();

  /**
   * When did this message start.
   */
  public final Date startedAt = new Date();

  /**
   * The HTTP headers of this message.
   */
  public final List<HttpHeader> headers = new ArrayList<HttpHeader>();

  /**
   * When did this message end.
   */
  public Date endedAt;

  /**
   * Attachments not used by HttProxy.
   */
  public final Map<String, Object> attachements = new HashMap<String, Object>();


  /**
   * The size of the body in bytes. May be null if the content of the response is not known from the header and has not been processed yet.
   */
  public Long bodySize;

  /**
   *
   * @param name The header name
   * @return True if at least one header in this message has the given name (case-insensitive). False otherwise.
   */
  public boolean hasHeader(String name)
  {
    for (HttpHeader header : headers)
    {
      if (header.name.equalsIgnoreCase(name))
      {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @param name The header name
   * @return All headers that have the given <i>name</i>.
   */
  public List<HttpHeader> getHeaders(String name)
  {
    List<HttpHeader> headersWithName = new ArrayList<HttpHeader>();
    for (HttpHeader header : headers)
    {
      if (header.name.equalsIgnoreCase(name))
      {
        headersWithName.add(header);
      }
    }
    return headersWithName;
  }

  /**
   *
   * @param name The HTTP header name to retrieve.
   * @return The value from the first header found that matches the given <i>name</i> or <code>null</code> if no match is found.
   */
  public String getHeaderValue(String name)
  {
    for (HttpHeader header : headers)
    {
      if (header.name.equalsIgnoreCase(name))
      {
        return header.value;
      }
    }

    return null;
  }

  /**
   * Keep-Alive: timeout=5, max=99
   * @return Returns the number of timeout seconds specified in the Keep-Alive or <code>null</code> if no timeout is specified.
   */
  public Integer getKeepAliveTimeoutSeconds()
  {
    for (HttpHeader header : headers)
    {
      if (header.name.equalsIgnoreCase("Keep-Alive"))
      {
        int index = header.value.indexOf("timeout=");
        if (index >= 0)
        {
          String timeout = header.value.substring(index + "timeout=".length());
          index = timeout.indexOf(",");
          if (index > 0)
          {
            timeout = timeout.substring(0, index);
          }

          try
          {
            return new Integer(timeout);
          }
          catch (NumberFormatException e)
          {}
        }
      }
    }

    return null;
  }

  /**
   * If a header with the given <i>name</i> exists then the value for this header is replaced. If no header matches the given
   * <i>name</i> then a new header is added with the given <i>value</i>.
   *
   * @param name The HTTP header name.
   * @param value The HTTP header value.
   */
  public void setOrAddHeader(String name, String value)
  {
    for (HttpHeader header : headers)
    {
      if (header.name.equalsIgnoreCase(name))
      {
        header.value = value;
        return;
      }
    }

    headers.add(new HttpHeader(name, value));
  }

  /**
   * Removes all HTTP headers with the given <i>name</i>.
   *
   * @param name The HTTP header name to remove.
   */
  public void removeHeader(String name)
  {
    for (int i = headers.size() - 1; i >= 0; i--)
    {
      if (headers.get(i).name.equalsIgnoreCase(name))
      {
        headers.remove(i);
      }
    }
  }

  @Override
  public String toString()
  {
    return new String(toBytes());
  }

  byte[] toBytes()
  {
    TByteArrayList bytes = new TByteArrayList((headers.size() + 1) * AVERAGE_HEADER_LENGTH);
    bytes.add(getStatusLine().getBytes());
    bytes.add(CR);
    bytes.add(LF);

    for (HttpHeader header : headers)
    {
      header.addTo(bytes);
    }

    bytes.add(CR);
    bytes.add(LF);

    return bytes.toArray();
  }

  HttpHeader addHeader(String headerLine)
  {
    int index = headerLine.indexOf(':');
    if ((index > 0) && (index < (headerLine.length() - 1)))
    {
      String name = headerLine.substring(0, index).trim();
      String value = headerLine.substring(index + 1, headerLine.length()).trim();
      HttpHeader header = new HttpHeader(name, value);
      headers.add(header);
      return header;
    }
    else
    {
      return null;
    }
  }
}

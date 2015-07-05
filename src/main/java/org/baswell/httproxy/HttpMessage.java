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
import java.util.List;
import static org.baswell.httproxy.Constants.*;

abstract public class HttpMessage
{
  abstract public String getStatusLine();

  public final Date startedAt = new Date();

  public List<HttpHeader> headers = new ArrayList<HttpHeader>();

  public Date endedAt;

  public Object attachement;

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

}

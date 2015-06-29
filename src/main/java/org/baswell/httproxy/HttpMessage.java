package org.baswell.httproxy;

import gnu.trove.list.array.TByteArrayList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static org.baswell.httproxy.Constants.*;

abstract public class HttpMessage
{
  abstract String getStatusLine();

  public final Date startedAt = new Date();

  public List<Header> headers = new ArrayList<Header>();

  public Date endedAt;

  public Object attachement;

  Header addHeader(String headerLine)
  {
    int index = headerLine.indexOf(':');
    if ((index > 0) && (index < (headerLine.length() - 1)))
    {
      String name = headerLine.substring(0, index).trim();
      String value = headerLine.substring(index + 1, headerLine.length()).trim();
      Header header = new Header(name, value);
      headers.add(header);
      return header;
    }
    else
    {
      return null;
    }
  }

  byte[] toBytes()
  {
    TByteArrayList bytes = new TByteArrayList((headers.size() + 1) * AVERAGE_HEADER_LENGTH);
    bytes.add(getStatusLine().getBytes());
    bytes.add(CR);
    bytes.add(LF);

    for (Header header : headers)
    {
      header.addTo(bytes);
    }

    bytes.add(CR);
    bytes.add(LF);

    return bytes.toArray();

  }

}

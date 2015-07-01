package org.baswell.httproxy;

import java.io.IOException;

abstract class HttpResponsePipe extends HttpMessagePipe
{
  abstract void onResponse(HttpResponse response) throws IOException, EndProxiedRequestException;

  HttpResponse currentResponse;

  HttpResponsePipe(ProxyDirector proxyDirector)
  {
    super(proxyDirector);
  }

  @Override
  void readStatusLine() throws IOException
  {
    byte[] statusLine = readNextLine();
    if (statusLine != null)
    {
      readBuffer.mark();
      currentMessage = currentResponse = new HttpResponse(new String(statusLine).trim());
      readState = ReadState.READING_HEADER;
    }
  }

  @Override
  void onHeadersProcessed() throws IOException, EndProxiedRequestException
  {
    onResponse(currentResponse);
  }
}

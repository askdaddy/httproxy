package org.baswell.httproxy;

import java.io.IOException;

abstract public class HttpRequestPipe extends HttpMessagePipe
{
  abstract void onRequest(HttpRequest request) throws EndProxiedRequestException, IOException;

  HttpRequest currentRequest;

  HttpRequestPipe(ProxyDirector proxyDirector)
  {
    super(proxyDirector);
  }

  @Override
  void readStatusLine() throws IOException
  {
    byte[] statusLine = readNextLine();
    if (statusLine != null)
    {
      currentMessage = currentRequest = new HttpRequest(new String(statusLine).trim());
      readState = ReadState.READING_HEADER;
    }
  }

  @Override
  void onHeadersProcessed()
  {
    onRequest(currentRequest);
  }
}

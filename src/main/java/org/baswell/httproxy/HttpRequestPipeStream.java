package org.baswell.httproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.baswell.httproxy.HttpMessageStreamMethods.*;

public class HttpRequestPipeStream extends HttpRequestPipe
{
  final HttpExchangeStream exchangeStream;

  final InputStream inputStream;

  OutputStream currentOutputStream;

  byte[] readBytes;

  int sleepSecondsOnReadWait;

  HttpRequestPipeStream(IOProxyDirector proxyDirector, HttpExchangeStream exchangeStream, InputStream inputStream)
  {
    super(proxyDirector);
    this.exchangeStream = exchangeStream;
    this.inputStream= inputStream;

    readBytes = new byte[bufferSize];
    sleepSecondsOnReadWait = proxyDirector.getSleepSecondsOnReadWait();
  }

  void readAndWriteMessage() throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    doReadAndWriteMessage(this, inputStream, readBytes, sleepSecondsOnReadWait);
  }

  @Override
  protected boolean write() throws ProxiedIOException
  {
    return doWrite(this, currentOutputStream, readBytes);
  }

  @Override
  void onRequest(HttpRequest request) throws EndProxiedRequestException, IOException
  {
    exchangeStream.onRequest();
  }

  @Override
  void onMessageDone() throws IOException
  {
    currentOutputStream.write(currentRequest.toBytes());
    exchangeStream.onRequestDone();
  }
}

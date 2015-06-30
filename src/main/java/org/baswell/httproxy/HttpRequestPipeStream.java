package org.baswell.httproxy;

import java.io.InputStream;
import java.io.OutputStream;

import static org.baswell.httproxy.StreamMessageMethods.*;

public class HttpRequestPipeStream extends HttpRequestPipe
{
  final ExchangeStream exchangeStream;

  final InputStream inputStream;

  OutputStream outputStream;

  byte[] readBytes;

  int sleepSecondsOnReadWait;

  HttpRequestPipeStream(IOProxyDirector proxyDirector, ExchangeStream exchangeStream, InputStream inputStream)
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
    return doWrite(this, outputStream, readBytes);
  }

  @Override
  void onRequest(HttpRequest request)
  {
    outputStream = exchangeStream.onRequest(currentRequest);
  }

  @Override
  void onMessageDone()
  {
    exchangeStream.onRequestDone(currentRequest);
  }
}

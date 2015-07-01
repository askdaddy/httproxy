package org.baswell.httproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.baswell.httproxy.HttpMessageStreamMethods.*;

public class HttpResponsePipeStream extends HttpResponsePipe
{
  final HttpExchangeStream exchangeStream;

  final OutputStream outputStream;

  InputStream currentInputStream;

  byte[] readBytes;

  final int sleepSecondsOnReadWait;

  HttpResponsePipeStream(IOProxyDirector proxyDirector, HttpExchangeStream exchangeStream, OutputStream outputStream)
  {
    super(proxyDirector);

    this.exchangeStream = exchangeStream;
    this.outputStream = outputStream;

    readBytes = new byte[bufferSize];
    sleepSecondsOnReadWait = proxyDirector.getSleepSecondsOnReadWait();
  }

  void readAndWriteMessage() throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    doReadAndWriteMessage(this, currentInputStream, readBytes, sleepSecondsOnReadWait);
  }

  @Override
  protected boolean write() throws ProxiedIOException
  {
    return doWrite(this, outputStream, readBytes);
  }

  @Override
  void onResponse(HttpResponse response) throws IOException, EndProxiedRequestException
  {
    exchangeStream.onResponse();
    outputStream.write(currentResponse.toBytes());
  }

  @Override
  void onMessageDone() throws IOException
  {
    exchangeStream.onResponseDone();
  }
}

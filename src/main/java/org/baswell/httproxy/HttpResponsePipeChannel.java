package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.baswell.httproxy.HttpMessageChannelMethods.*;


public class HttpResponsePipeChannel extends HttpResponsePipe
{
  private final HttpExchangeChannel exchangeChannel;

  private final SocketChannel writeChannel;

  private final int maxWriteAttempts;

  SocketChannel currentReadChannel;

  HttpResponsePipeChannel(NIOProxyDirector proxyDirector, HttpExchangeChannel exchangeChannel, SocketChannel writeChannel)
  {
    super(proxyDirector);

    this.exchangeChannel = exchangeChannel;
    this.writeChannel = writeChannel;
    this.maxWriteAttempts = proxyDirector.getMaxWriteAttempts();
  }

  boolean readAndWriteAvailabe() throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    return doReadAndWriteAvailabe(this, readBuffer, currentReadChannel);
  }

  @Override
  boolean write() throws ProxiedIOException
  {
    return doWrite(this, writeChannel, writeBuffer, readBuffer, maxWriteAttempts);
  }

  @Override
  void onResponse(HttpResponse response) throws IOException, EndProxiedRequestException
  {
    exchangeChannel.onResponse();
    writeBuffer.add(currentResponse.toBytes());
  }

  @Override
  void onMessageDone()
  {
    exchangeChannel.onRequestDone();
  }
}

package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.baswell.httproxy.HttpMessageChannelMethods.*;


public class HttpResponsePipeChannel extends HttpResponsePipe
{
  final HttpExchangeChannel exchangeChannel;

  final SocketChannel writeChannel;

  final int maxWriteAttempts;

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
  protected boolean write() throws ProxiedIOException
  {
    return doWrite(this, writeChannel, writeBuffer, readBuffer, maxWriteAttempts);
  }

  @Override
  void onResponse(HttpResponse response) throws IOException, EndProxiedRequestException
  {
    exchangeChannel.onRequest();
  }

  @Override
  void onMessageDone()
  {
    exchangeChannel.onRequestDone();
  }
}

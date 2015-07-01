package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.baswell.httproxy.HttpMessageChannelMethods.*;


public class HttpRequestPipeChannel extends HttpRequestPipe
{
  final HttpExchangeChannel exchangeChannel;

  final SocketChannel readChannel;

  final int maxWriteAttempts;

  SocketChannel currentWriteChannel;

  HttpRequestPipeChannel(NIOProxyDirector proxyDirector, HttpExchangeChannel exchangeChannel, SocketChannel readChannel)
  {
    super(proxyDirector);

    this.exchangeChannel = exchangeChannel;
    this.readChannel = readChannel;
    this.maxWriteAttempts = proxyDirector.getMaxWriteAttempts();
  }

  boolean readAndWriteAvailabe() throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    return doReadAndWriteAvailabe(this, readBuffer, readChannel);
  }

  @Override
  protected boolean write() throws ProxiedIOException
  {
    return doWrite(this, currentWriteChannel, writeBuffer, readBuffer, maxWriteAttempts);
  }

  @Override
  void onRequest(HttpRequest request) throws EndProxiedRequestException, IOException
  {
    writeBuffer.add(currentRequest.toBytes());
    exchangeChannel.onRequest();
  }

  @Override
  void onMessageDone()
  {
    exchangeChannel.onRequestDone();
  }
}

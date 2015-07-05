package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.baswell.httproxy.HttpMessageChannelMethods.*;


public class PipedResponseChannel extends PipedResponse
{
  private final PipedExchangeChannel pipedExchangeChannel;

  private final SocketChannel writeChannel;

  private final int maxWriteAttempts;

  SocketChannel currentReadChannel;

  boolean overSSL;

  PipedResponseChannel(NIOProxyDirector proxyDirector, PipedExchangeChannel pipedExchangeChannel, SocketChannel writeChannel)
  {
    super(proxyDirector);

    this.pipedExchangeChannel = pipedExchangeChannel;
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
    pipedExchangeChannel.onResponse();
    writeBuffer.add(currentResponse.toBytes());
  }

  @Override
  void onMessageDone()
  {
    pipedExchangeChannel.onRequestDone();
  }

  @Override
  boolean overSSL()
  {
    return overSSL;
  }

}

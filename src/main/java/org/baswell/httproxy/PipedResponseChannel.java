/*
 * Copyright 2015 Corey Baswell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.baswell.httproxy.PipedMessageChannelMethods.*;

class PipedResponseChannel extends PipedResponse
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
    pipedExchangeChannel.onResponseDone();
  }

  @Override
  boolean overSSL()
  {
    return overSSL;
  }

}

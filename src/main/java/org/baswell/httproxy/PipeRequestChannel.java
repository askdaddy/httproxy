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

import static org.baswell.httproxy.HttpMessageChannelMethods.*;

public class PipeRequestChannel extends PipedRequest
{
  private final PipedExchangeChannel pipedExchangeChannel;

  private final SocketChannel readChannel;

  private final int maxWriteAttempts;

  SocketChannel currentWriteChannel;

  PipeRequestChannel(NIOProxyDirector proxyDirector, PipedExchangeChannel pipedExchangeChannel, SocketChannel readChannel)
  {
    super(proxyDirector);

    this.pipedExchangeChannel = pipedExchangeChannel;
    this.readChannel = readChannel;
    this.maxWriteAttempts = proxyDirector.getMaxWriteAttempts();
  }

  boolean readAndWriteAvailabe() throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    return doReadAndWriteAvailabe(this, readBuffer, readChannel);
  }

  @Override
  boolean write() throws ProxiedIOException
  {
    return doWrite(this, currentWriteChannel, writeBuffer, readBuffer, maxWriteAttempts);
  }

  @Override
  void onRequest(HttpRequest request) throws EndProxiedRequestException, IOException
  {
    writeBuffer.add(currentRequest.toBytes());
    pipedExchangeChannel.onRequest();
  }

  @Override
  void onMessageDone()
  {
    pipedExchangeChannel.onRequestDone();
  }
}

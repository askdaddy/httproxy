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

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

class SocketChannelMultiplexer extends ConnectionMultiplexer<SocketChannel>
{
  private final ExecutorService executorService;

  private final ProxyLogger logger;

  SocketChannelMultiplexer(ExecutorService executorService, ProxyLogger logger)
  {
    this.executorService = executorService;
    this.logger = new WrappedLogger(logger);
  }

  @Override
  protected SocketChannel connect(ConnectionParameters connectionParameters) throws IOException
  {
    InetSocketAddress address = new InetSocketAddress(connectionParameters.ipOrHost, connectionParameters.port);
    SocketChannel socketChannel = SocketChannel.open(address);
    socketChannel.configureBlocking(false);

    if (connectionParameters.ssl)
    {
      SSLEngine sslEngine = connectionParameters.sslContext.createSSLEngine(address.getHostName(), address.getPort());
      sslEngine.setUseClientMode(true);
      socketChannel = new SSLSocketChannel(socketChannel, sslEngine, executorService, logger);
    }

    return socketChannel;
  }

  @Override
  protected void closeQuitely(SocketChannel socketChannel)
  {
    try
    {
      if (socketChannel.isOpen())
      {
        socketChannel.close();
      }
    }
    catch (IOException e)
    {}
  }
}

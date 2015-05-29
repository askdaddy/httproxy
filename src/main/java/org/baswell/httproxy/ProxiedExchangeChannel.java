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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class ProxiedExchangeChannel
{
  final SelectorLoop selectorLoop;

  final SocketChannel clientSocketChannel;

  final ProxiedRequestChannel requestChannel;

  final SelectionKey requestSelectionKey;

  final NIOProxyDirector proxyDirector;

  boolean connectingServerChannel;

  SocketChannel serverSocketChannel;

  ProxiedResponseChannel responseChannel;

  SelectionKey responseSelectionKey;

  ProxiedExchangeChannel(SelectorLoop selectorLoop, SocketChannel clientSocketChannel, NIOProxyDirector proxyDirector) throws IOException
  {
    this.selectorLoop = selectorLoop;
    this.clientSocketChannel = clientSocketChannel;
    this.proxyDirector = proxyDirector;

    clientSocketChannel.configureBlocking(false);



    SocketChannel realSocketChannel;
    if (clientSocketChannel instanceof WrappedSocketChannel)
    {
      realSocketChannel = ((WrappedSocketChannel)clientSocketChannel).getWrappedSocketChannel();
    }
    else
    {
      realSocketChannel = clientSocketChannel;
    }

    requestSelectionKey = realSocketChannel.register(selectorLoop.selector, SelectionKey.OP_READ);
    requestSelectionKey.attach(this);
    this.requestChannel = new ProxiedRequestChannel(this, clientSocketChannel, proxyDirector);
  }

  void onReadReady(SelectionKey selectionKey)
  {
    connectingServerChannel = false;
    try
    {
      if (requestSelectionKey == selectionKey)
      {
        if (!requestChannel.readAndWriteAvailabe())
        {
          responseSelectionKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }
        else if ((responseSelectionKey != null) && ((responseSelectionKey.interestOps() & SelectionKey.OP_WRITE) != 0))
        {
          responseSelectionKey.interestOps(SelectionKey.OP_READ);
        }
      }
      else if (responseSelectionKey == selectionKey)
      {
        if (!responseChannel.readAndWriteAvailabe())
        {
          requestSelectionKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }
        else
        {
          if ((requestSelectionKey.interestOps() & SelectionKey.OP_WRITE) != 0)
          {
            requestSelectionKey.interestOps(SelectionKey.OP_READ);
          }

          if (responseChannel.isMessageComplete())
          {
            proxyDirector.onExchangeComplete(requestChannel, responseChannel);
          }
        }
      }
      else
      {
        // TODO Error
      }
    }
    catch (ProxiedIOException proxiedIOException)
    {
      if (connectingServerChannel)
      {
        connectingServerChannel = false;
      }
      else if (!requestChannel.isMessageComplete() || !responseChannel.isMessageComplete())
      {
        if (proxiedIOException.request)
        {
          proxyDirector.onPrematureRequestClosed(requestChannel, proxiedIOException.e);
        }
        else
        {
          proxyDirector.onPrematureResponseClosed(requestChannel, responseChannel, proxiedIOException.e);
        }
      }

      close();
    }
    catch (HttpProtocolException e)
    {
      if (e.request)
      {
        proxyDirector.onRequestHttpProtocolError(requestChannel, e.getMessage());
      }
      else
      {
        proxyDirector.onResponseHttpProtocolError(requestChannel, responseChannel, e.getMessage());
      }
      close();
    }
    catch (EndProxiedRequestException e)
    {
      try
      {
        clientSocketChannel.write(ByteBuffer.wrap(e.toString().getBytes()));
      }
      catch (IOException ie)
      {}
      close();
    }
  }

  /*
   * If a previous onReadReady event could not write all the output to the socket channel buffer we'll start listening
   * for write ready events for that socket channel. When ready this method will be called and if a full write is performed
   * we'll turn off the write ready events.
   */
  void onWriteReady(SelectionKey selectionKey)
  {
    try
    {
      if (selectionKey == requestSelectionKey)
      {
        if (responseChannel.write())
        {
          requestSelectionKey.interestOps(SelectionKey.OP_READ);
        }
      }
      else
      {
        if (requestChannel.write())
        {
          responseSelectionKey.interestOps(SelectionKey.OP_READ);

          if (responseChannel.isMessageComplete())
          {
            proxyDirector.onExchangeComplete(requestChannel, responseChannel);
          }
        }
      }
    }
    catch (ProxiedIOException proxiedIOException)
    {
      if (!requestChannel.isMessageComplete() || !responseChannel.isMessageComplete())
      {
        if (proxiedIOException.request)
        {
          proxyDirector.onPrematureRequestClosed(requestChannel, proxiedIOException.e);
        }
        else
        {
          proxyDirector.onPrematureResponseClosed(requestChannel, responseChannel, proxiedIOException.e);
        }
      }

      close();
    }
  }

  SocketChannel connectProxiedServer() throws IOException, EndProxiedRequestException
  {
    connectingServerChannel = true;
    serverSocketChannel = proxyDirector.connectToProxiedHost(requestChannel);
    if (serverSocketChannel == null)
    {
      throw EndProxiedRequestException.NOT_FOUND;
    }

    SocketChannel realSocketChannel;
    if (serverSocketChannel instanceof WrappedSocketChannel)
    {
      realSocketChannel = ((WrappedSocketChannel)serverSocketChannel).getWrappedSocketChannel();
    }
    else
    {
      realSocketChannel = serverSocketChannel;
    }

    serverSocketChannel.configureBlocking(false);
    responseSelectionKey = realSocketChannel.register(selectorLoop.selector, SelectionKey.OP_READ);
    responseSelectionKey.attach(this);
    responseChannel = new ProxiedResponseChannel(this, serverSocketChannel, requestChannel.readChannel, proxyDirector);
    connectingServerChannel = false;
    return serverSocketChannel;
  }

  void close()
  {
    if (clientSocketChannel.isConnected())
    {
      try
      {
        clientSocketChannel.close();
      }
      catch (Exception e)
      {
      }
    }

    if ((serverSocketChannel != null) && serverSocketChannel.isConnected())
    {
      try
      {
        serverSocketChannel.close();
      }
      catch (Exception e)
      {}
    }
  }
}

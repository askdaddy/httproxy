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
import java.util.HashMap;
import java.util.Map;

class PipedExchangeChannel
{
  private final SelectorLoop selectorLoop;

  private final SocketChannel clientSocketChannel;

  private final PipeRequestChannel requestPipeChannel;

  private final PipedResponseChannel responsePipeChannel;

  private final SelectionKey requestSelectionKey;

  private final NIOProxyDirector proxyDirector;

  private final ProxyLogger log;

  private final SocketChannelMultiplexer socketChannelMultiplexer;

  private boolean connectingServerChannel;

  private final Map<ConnectionParameters, SelectionKey> responseSelectionKeys = new HashMap<ConnectionParameters, SelectionKey>();

  private SelectionKey currentResponseSelectionKey;

  private ConnectionParameters currentConnectionParameters;

  PipedExchangeChannel(SelectorLoop selectorLoop, SocketChannel clientSocketChannel, NIOProxyDirector proxyDirector) throws IOException
  {
    this.selectorLoop = selectorLoop;
    this.clientSocketChannel = clientSocketChannel;
    this.proxyDirector = proxyDirector;

    log = proxyDirector.getLogger();
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

    requestPipeChannel = new PipeRequestChannel(proxyDirector, this, clientSocketChannel);
    responsePipeChannel = new PipedResponseChannel(proxyDirector, this, clientSocketChannel);
    socketChannelMultiplexer = new SocketChannelMultiplexer(proxyDirector.getSSLThreadPool(), proxyDirector.getLogger());
  }

  void onReadReady(SelectionKey selectionKey)
  {
    connectingServerChannel = false;
    if (requestSelectionKey == selectionKey)
    {
      try
      {
        if (!requestPipeChannel.readAndWriteAvailabe())
        {
          requestSelectionKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }
        else if ((requestSelectionKey.interestOps() & SelectionKey.OP_WRITE) != 0)
        {
          requestSelectionKey.interestOps(SelectionKey.OP_READ);
        }
      }
      catch (ProxiedIOException proxiedIOException)
      {
        if (connectingServerChannel)
        {
          connectingServerChannel = false;
        }
        else if (!requestPipeChannel.isMessageComplete() || !responsePipeChannel.isMessageComplete())
        {
          proxyDirector.onPrematureRequestClosed(requestPipeChannel.currentRequest, proxiedIOException.e);
        }

        close();
      }
      catch (HttpProtocolException e)
      {
        proxyDirector.onRequestHttpProtocolError(requestPipeChannel.currentRequest, e.getMessage());
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
    else if (currentResponseSelectionKey == selectionKey)
    {
      try
      {
        if (!responsePipeChannel.readAndWriteAvailabe())
        {
          currentResponseSelectionKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }
        else
        {
          if ((currentResponseSelectionKey.interestOps() & SelectionKey.OP_WRITE) != 0)
          {
            currentResponseSelectionKey.interestOps(SelectionKey.OP_READ);
          }
        }
      }
      catch (ProxiedIOException proxiedIOException)
      {
        if (connectingServerChannel)
        {
          connectingServerChannel = false;
        }
        else if (!requestPipeChannel.isMessageComplete() || !responsePipeChannel.isMessageComplete())
        {
          proxyDirector.onPrematureResponseClosed(requestPipeChannel.currentRequest, responsePipeChannel.currentResponse, currentConnectionParameters, proxiedIOException.e);
        }

        close();
      }
      catch (HttpProtocolException e)
      {
        proxyDirector.onResponseHttpProtocolError(requestPipeChannel.currentRequest, responsePipeChannel.currentResponse, currentConnectionParameters, e.getMessage());
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
    else
    {
      if (log != null)
      {
        log.error("Received onReadReady() event with invalid selection key.");
      }
    }
  }

  /*
   * If a previous onReadReady event could not write all the output to the socket channel buffer we'll start listening
   * for write ready events for that socket channel. When ready this method will be called and if a full write is performed
   * we'll turn off the write ready events.
   */
  void onWriteReady(SelectionKey selectionKey)
  {
    if (selectionKey == requestSelectionKey)
    {
      try
      {
        if (requestPipeChannel.write())
        {
          requestSelectionKey.interestOps(SelectionKey.OP_READ);
        }
      }
      catch (ProxiedIOException proxiedIOException)
      {
        if (!requestPipeChannel.isMessageComplete() || !responsePipeChannel.isMessageComplete())
        {
          proxyDirector.onPrematureRequestClosed(requestPipeChannel.currentRequest, proxiedIOException.e);
        }

        close();
      }
    }
    else if (selectionKey == currentResponseSelectionKey)
    {
      try
      {
        if (responsePipeChannel.write())
        {
          currentResponseSelectionKey.interestOps(SelectionKey.OP_READ);
        }
      }
      catch (ProxiedIOException proxiedIOException)
      {
        if (!requestPipeChannel.isMessageComplete() || !responsePipeChannel.isMessageComplete())
        {
          proxyDirector.onPrematureResponseClosed(requestPipeChannel.currentRequest, responsePipeChannel.currentResponse, currentConnectionParameters, proxiedIOException.e);
        }

        close();
      }
    }
    else
    {
      if (log != null)
      {
        log.error("Received onWriteReady() event with invalid selection key.");
      }
    }
  }

  void onRequest() throws EndProxiedRequestException, IOException
  {
    connectingServerChannel = true;

    currentConnectionParameters = proxyDirector.onRequestStart(requestPipeChannel.currentRequest);
    if (currentConnectionParameters == null)
    {
      throw EndProxiedRequestException.NOT_FOUND;
    }

    SocketChannel serverSocketChannel = socketChannelMultiplexer.getConnectionFor(currentConnectionParameters);

    if (responseSelectionKeys.containsKey(currentConnectionParameters))
    {
      currentResponseSelectionKey = responseSelectionKeys.get(currentConnectionParameters);
    }
    else
    {
      SocketChannel realSocketChannel = (serverSocketChannel instanceof WrappedSocketChannel) ? ((WrappedSocketChannel)serverSocketChannel).getWrappedSocketChannel() : serverSocketChannel;
      currentResponseSelectionKey = realSocketChannel.register(selectorLoop.selector, SelectionKey.OP_READ);
      currentResponseSelectionKey.attach(this);
      responseSelectionKeys.put(currentConnectionParameters, currentResponseSelectionKey);
    }

    requestPipeChannel.currentWriteChannel = responsePipeChannel.currentReadChannel = serverSocketChannel;
    connectingServerChannel = false;
  }

  void onRequestDone()
  {
    proxyDirector.onRequestEnd(requestPipeChannel.currentRequest, currentConnectionParameters);
  }

  void onResponse()
  {
    proxyDirector.onResponseStart(requestPipeChannel.currentRequest, responsePipeChannel.currentResponse, currentConnectionParameters);
  }

  void onResponseDone()
  {
    proxyDirector.onResponseEnd(requestPipeChannel.currentRequest, responsePipeChannel.currentResponse, currentConnectionParameters);
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
      {}
    }

    socketChannelMultiplexer.closeQuitely();
  }
}

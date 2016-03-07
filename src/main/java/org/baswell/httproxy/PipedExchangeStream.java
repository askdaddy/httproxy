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
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

class PipedExchangeStream implements ReapedPipedExchange
{
  final SocketMultiplexer socketMultiplexer;

  volatile boolean closed;

  private final Socket clientSocket;

  private final IOProxyDirector proxyDirector;

  private final PipedRequestStream requestPipeStream;

  private final PipedResponseStream responsePipeStream;

  private final ClientOutputStream clientOutputStream;

  private ConnectionParameters currentConnectionParameters;

  private volatile boolean connectingServerSocket;

  private final CountDownLatch responseStartSignal = new CountDownLatch(1);

  private final ProxyLogger log;

  private ModifiedOutputStream requestModifiedOutputStream;

  Integer lastKeepAliveTimeoutSeconds;

  long lastExchangeAt;

  ModifiedOutputStream modifiedResponseStream;

  PipedExchangeStream(Socket clientSocket, IOProxyDirector proxyDirector) throws IOException
  {
    clientSocket.setKeepAlive(true); // Use keep alives so we know when the far end has shutdown the socket.

    this.clientSocket = clientSocket;
    this.proxyDirector = proxyDirector;

    this.log = new WrappedLogger(proxyDirector.getLogger());

    requestPipeStream = new PipedRequestStream(proxyDirector, this, clientSocket);
    clientOutputStream = new ClientOutputStream(clientSocket.getOutputStream());
    responsePipeStream = new PipedResponseStream(proxyDirector, this, clientOutputStream);

    socketMultiplexer = new SocketMultiplexer();
  }

  synchronized void requestLoop()
  {
    try
    {
      responseStartSignal.countDown();
      while (!closed)
      {
        requestPipeStream.reset();
        requestPipeStream.readAndWriteMessage();

        try
        {
          onRequestDone();
          notifyAll();
          if (!closed)
          {
            wait();
          }
        }
        catch (InterruptedException e)
        {
        }
        catch (IOException e)
        {
          throw new ProxiedIOException(requestPipeStream.currentRequest, true, e);
        }
      }
    }
    catch (ProxiedIOException proxiedIOException)
    {
      if (!closed)
      {
        if (!connectingServerSocket)
        {
          if (!requestPipeStream.isMessageComplete() || !responsePipeStream.isMessageComplete())
          {
            if (proxiedIOException.reading)
            {
              proxyDirector.onPrematureRequestClosed(requestPipeStream.currentRequest, proxiedIOException.e);
            }
            else
            {
              proxyDirector.onPrematureResponseClosed(requestPipeStream.currentRequest, responsePipeStream.currentResponse, proxiedIOException.e);
            }
          }
        }

        close();
      }
    }
    catch (HttpProtocolException e)
    {
      if (!closed)
      {
        proxyDirector.onRequestHttpProtocolError(requestPipeStream.currentRequest, e.getMessage());
        close();
      }
    }
    catch (EndProxiedRequestException e)
    {
      try
      {
        clientSocket.getOutputStream().write(e.toString().getBytes());
      }
      catch (IOException ie)
      {
      }
      close();
    }
    catch (Exception e)
    {
      log.error("Unexpected exception thrown in request loop.", e);
      close();
    }
  }

  void responseLoop()
  {
    try
    {
      responseStartSignal.await();
    }
    catch (InterruptedException e)
    {}

    synchronized (this)
    {
      try
      {
        while (!closed)
        {
          try
          {
            responsePipeStream.reset();
            responsePipeStream.readAndWriteMessage();
            onResponseDone();
            notifyAll();
            if (!closed)
            {
              wait();
            }
          }
          catch (InterruptedException e)
          {}
          catch (IOException e)
          {
            throw new ProxiedIOException(responsePipeStream.currentResponse, true, e);
          }
        }
      }
      catch (ProxiedIOException proxiedIOException)
      {
        if (!closed)
        {
          if (!connectingServerSocket)
          {
            if (!requestPipeStream.isMessageComplete() || !responsePipeStream.isMessageComplete())
            {
              if (proxiedIOException.reading)
              {
                proxyDirector.onPrematureResponseClosed(requestPipeStream.currentRequest, responsePipeStream.currentResponse, proxiedIOException.e);
              }
              else
              {
                proxyDirector.onPrematureRequestClosed(requestPipeStream.currentRequest, proxiedIOException.e);
              }
            }
          }

          close();
        }
      }
      catch (HttpProtocolException e)
      {
        if (!closed)
        {
          proxyDirector.onResponseHttpProtocolError(requestPipeStream.currentRequest, responsePipeStream.currentResponse, e.getMessage());
          close();
        }
      }
      catch (EndProxiedRequestException e)
      {
        /*
         * This should never be thrown here. Only on request side from ProxyDirector when trying to connect to proxied
         * server.
         */
        try
        {
          clientSocket.getOutputStream().write(e.toString().getBytes());
        }
        catch (IOException ie)
        {}
        close();
      }
      catch (Exception e)
      {
        log.error("Unexpected exception thrown in response loop.", e);
        close();
      }
    }
  }

  void onRequest() throws EndProxiedRequestException, IOException
  {
    currentConnectionParameters = proxyDirector.onRequestStart(requestPipeStream.currentRequest);
    if (currentConnectionParameters == null)
    {
      throw EndProxiedRequestException.NOT_FOUND;
    }
    else
    {
      connectingServerSocket = true;
      try
      {
        Socket serverSocket = socketMultiplexer.getConnectionFor(currentConnectionParameters);
        serverSocket.setKeepAlive(true); // Use keep alives so we know when the far end has shutdown the socket.
        Integer socketTimeout = proxyDirector.getSocketReadTimeoutMilliseconds();
        if (socketTimeout != null && socketTimeout > 0)
        {
          serverSocket.setSoTimeout(socketTimeout);
        }

        requestPipeStream.currentOutputStream = serverSocket.getOutputStream();

        responsePipeStream.currentConnectionParameters = currentConnectionParameters;
        responsePipeStream.currentInputStream = serverSocket.getInputStream();
        responsePipeStream.overSSL = currentConnectionParameters.ssl;

        connectingServerSocket = false;

        if (requestPipeStream.currentRequest.hasContent())
        {
          RequestContentModifier contentModifier = proxyDirector.getRequestModifier(requestPipeStream.currentRequest);
          if (contentModifier != null)
          {
            requestModifiedOutputStream = new ModifiedOutputStream(requestPipeStream.currentRequest, contentModifier, requestPipeStream.currentOutputStream, 20, log);
          }
        }

      }
      catch (IOException e)
      {
        proxyDirector.onConnectionFailed(requestPipeStream.currentRequest, currentConnectionParameters, e);
        throw e;
      }
    }
  }

  void onRequestHeaderWritten() throws IOException
  {
    if (requestModifiedOutputStream != null)
    {
      requestPipeStream.currentOutputStream = requestModifiedOutputStream;
    }
  }

  void onRequestDone() throws IOException
  {
    if (requestModifiedOutputStream != null)
    {
      requestModifiedOutputStream.onContentComplete();
    }

    proxyDirector.onRequestEnd(requestPipeStream.currentRequest, currentConnectionParameters);
    requestPipeStream.currentOutputStream.flush();
    requestPipeStream.currentOutputStream = null;
    requestModifiedOutputStream = null;
  }

  void onResponse() throws EndProxiedRequestException, IOException
  {
    proxyDirector.onResponseStart(requestPipeStream.currentRequest, responsePipeStream.currentResponse);
    if (responsePipeStream.currentResponse.hasContent())
    {
      ResponseContentModifier responseContentModifier = proxyDirector.getResponseModifier(requestPipeStream.currentRequest, responsePipeStream.currentResponse);
      modifiedResponseStream = responseContentModifier != null ? new ModifiedOutputStream(requestPipeStream.currentRequest, responsePipeStream.currentResponse, responseContentModifier, clientOutputStream.rawOutputStream, 20, log) : null;
    }
    else
    {
      modifiedResponseStream = null;
    }
  }

  void onResponseHeaderSent()
  {
    clientOutputStream.wrappedOutputStream = modifiedResponseStream;
  }

  synchronized void onResponseDone() throws IOException
  {
    if (modifiedResponseStream != null)
    {
      modifiedResponseStream.onContentComplete();
    }

    lastExchangeAt = System.currentTimeMillis();
    lastKeepAliveTimeoutSeconds = responsePipeStream.currentResponse.getKeepAliveTimeoutSeconds();

    responsePipeStream.outputStream.flush();
    proxyDirector.onResponseEnd(requestPipeStream.currentRequest, responsePipeStream.currentResponse);
    responsePipeStream.currentInputStream = null;
    clientOutputStream.wrappedOutputStream = null;

    /*
     * Some browsers (Firefox, Safari) will keep the connection open until the server actually closes it. Since we'd are about
     * to go into a read wait on the client (request) if the server connection is going to shutdown anyway go ahead and close shop.
     */
    if ("close".equalsIgnoreCase(responsePipeStream.currentMessage.getHeaderValue("Connection")))
    {
      close();
    }
  }

  @Override
  public boolean active()
  {
    return requestPipeStream.readState != PipedMessage.ReadState.READING_STATUS;
  }

  @Override
  public Integer getLastKeepAliveTimeoutSeconds()
  {
    return lastKeepAliveTimeoutSeconds;
  }

  @Override
  public long getLastExchangeAt()
  {
    return lastExchangeAt;
  }

  @Override
  public boolean closed()
  {
    return closed;
  }

  @Override
  public void close()
  {
    if (!closed)
    {
      closed = true;
      if (clientSocket.isConnected())
      {
        try
        {
          clientSocket.close();
        }
        catch (Exception e)
        {}
      }

      synchronized (this)
      {
        notifyAll();
      }

      socketMultiplexer.closeQuitely();
    }
  }
}

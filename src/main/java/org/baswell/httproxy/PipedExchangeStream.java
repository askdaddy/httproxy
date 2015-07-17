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

class PipedExchangeStream
{
  private final Socket clientSocket;

  private final IOProxyDirector proxyDirector;

  private final PipedRequestStream requestPipeStream;

  private final PipedResponseStream responsePipeStream;

  private final SocketMultiplexer socketMultiplexer;

  private ConnectionParameters currentConnectionParameters;

  private volatile boolean connectingServerSocket;

  private volatile boolean closed;

  private final CountDownLatch responseStartSignal = new CountDownLatch(1);

  private final ProxyLogger log;

  PipedExchangeStream(Socket clientSocket, IOProxyDirector proxyDirector) throws IOException
  {
    clientSocket.setKeepAlive(true); // Use keep alives so we know when the far end has shutdown the socket.

    this.clientSocket = clientSocket;
    this.proxyDirector = proxyDirector;

    ProxyLogger log = proxyDirector.getLogger();
    this.log = log == null ? new DevNullLogger() : log;

    requestPipeStream = new PipedRequestStream(proxyDirector, this, clientSocket);
    responsePipeStream = new PipedResponseStream(proxyDirector, this, clientSocket.getOutputStream());

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
          throw new ProxiedIOException(requestPipeStream.currentRequest, e);
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
            proxyDirector.onPrematureRequestClosed(requestPipeStream.currentRequest, proxiedIOException.e);
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
            throw new ProxiedIOException(responsePipeStream.currentResponse, e);
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
              proxyDirector.onPrematureResponseClosed(requestPipeStream.currentRequest, responsePipeStream.currentResponse, currentConnectionParameters, proxiedIOException.e);
            }
          }

          close();
        }
      }
      catch (HttpProtocolException e)
      {
        if (!closed)
        {
          proxyDirector.onResponseHttpProtocolError(requestPipeStream.currentRequest, responsePipeStream.currentResponse, currentConnectionParameters, e.getMessage());
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

        requestPipeStream.currentOutputStream = serverSocket.getOutputStream();

        responsePipeStream.currentInputStream = serverSocket.getInputStream();
        responsePipeStream.overSSL = currentConnectionParameters.ssl;

        connectingServerSocket = false;
      }
      catch (IOException e)
      {
        proxyDirector.onConnectionFailed(requestPipeStream.currentRequest, currentConnectionParameters, e);
        throw e;
      }
    }
  }

  void onRequestDone() throws IOException
  {
    proxyDirector.onRequestEnd(requestPipeStream.currentRequest, currentConnectionParameters);
    requestPipeStream.currentOutputStream.flush();
    requestPipeStream.currentOutputStream = null;
  }

  void onResponse() throws EndProxiedRequestException, IOException
  {
    proxyDirector.onResponseStart(requestPipeStream.currentRequest, responsePipeStream.currentResponse, currentConnectionParameters);
  }

  synchronized void onResponseDone() throws IOException
  {
    responsePipeStream.outputStream.flush();
    proxyDirector.onResponseEnd(requestPipeStream.currentRequest, responsePipeStream.currentResponse, currentConnectionParameters);
    responsePipeStream.currentInputStream = null;
  }

  void close()
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
        {
        }
      }

      synchronized (this)
      {
        notifyAll();
      }

      socketMultiplexer.closeQuitely();
    }
  }
}

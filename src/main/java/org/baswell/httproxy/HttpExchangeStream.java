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

class HttpExchangeStream
{
  final Socket clientSocket;

  final IOProxyDirector proxyDirector;

  final HttpRequestPipeStream requestPipeStream;

  final HttpResponsePipeStream responsePipeStream;

  final SocketMultiplexer socketMultiplexer;

  ConnectionParameters currentConnectionParameters;

  volatile boolean connectingServerSocket;

  volatile boolean closed;

  HttpExchangeStream(Socket clientSocket, IOProxyDirector proxyDirector) throws IOException
  {
    this.clientSocket = clientSocket;
    this.proxyDirector = proxyDirector;


    requestPipeStream = new HttpRequestPipeStream(proxyDirector, this, clientSocket.getInputStream());
    responsePipeStream = new HttpResponsePipeStream(proxyDirector, this, clientSocket.getOutputStream());

    socketMultiplexer = new SocketMultiplexer();
  }

  void requestLoop()
  {
    try
    {
      while (!closed)
      {
        requestPipeStream.readAndWriteMessage();
      }
    }
    catch (ProxiedIOException proxiedIOException)
    {
      synchronized (this)
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
    }
    catch (HttpProtocolException e)
    {
      synchronized (this)
      {
        if (!closed)
        {
          proxyDirector.onRequestHttpProtocolError(requestPipeStream.currentRequest, e.getMessage());
          close();
        }
      }
    }
    catch (EndProxiedRequestException e)
    {
      try
      {
        clientSocket.getOutputStream().write(e.toString().getBytes());
      }
      catch (IOException ie)
      {}
      close();
    }
  }

  void responseLoop()
  {
    try
    {
      while (!closed)
      {
        synchronized (this)
        {
          if (responsePipeStream.currentInputStream == null)
          {
            try
            {
              wait();
            }
            catch (InterruptedException e)
            {}
          }
        }

        if (!closed && responsePipeStream.currentInputStream != null)
        {
          responsePipeStream.readAndWriteMessage();
        }
      }
    }
    catch (ProxiedIOException proxiedIOException)
    {
      synchronized (this)
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
    }
    catch (HttpProtocolException e)
    {
      synchronized (this)
      {
        if (!closed)
        {
          proxyDirector.onResponseHttpProtocolError(requestPipeStream.currentRequest, responsePipeStream.currentResponse, currentConnectionParameters, e.getMessage());
          close();
        }
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
  }

  void onRequest() throws EndProxiedRequestException, IOException
  {
    currentConnectionParameters = proxyDirector.onRequest(requestPipeStream.currentRequest);
    if (currentConnectionParameters == null)
    {
      throw EndProxiedRequestException.NOT_FOUND;
    }
    else
    {
      connectingServerSocket = true;
      Socket serverSocket = socketMultiplexer.getConnectionFor(currentConnectionParameters);
      connectingServerSocket = false;
      requestPipeStream.currentOutputStream = serverSocket.getOutputStream();
      responsePipeStream.currentInputStream = serverSocket.getInputStream();


      synchronized (this)
      {
        notifyAll();
      }
    }
  }

  void onRequestDone()
  {
    proxyDirector.onRequestDone(requestPipeStream.currentRequest, currentConnectionParameters);
    requestPipeStream.currentOutputStream = null;
  }

  void onResponse() throws EndProxiedRequestException, IOException
  {
    proxyDirector.onResponse(requestPipeStream.currentRequest, responsePipeStream.currentResponse, currentConnectionParameters);
  }

  void onResponseDone()
  {
    proxyDirector.onExchangeComplete(requestPipeStream.currentRequest, responsePipeStream.currentResponse, currentConnectionParameters);
    responsePipeStream.currentInputStream = null;
  }

  void close()
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
      notify();
    }

    socketMultiplexer.closeQuitely();
  }
}

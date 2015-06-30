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

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

class ExchangeStream
{
  private Socket proxiedClientSocket;

  protected final ProxiedRequestStream requestStream;

  private Socket proxiedServerSocket;

  protected volatile ProxiedResponseStream responseStream;

  private final IOProxyDirector proxyDirector;

  private boolean connectingServerSocket;

  private volatile boolean closed;

  ExchangeStream(Socket proxiedClientSocket, IOProxyDirector proxyDirector) throws IOException
  {
    this.proxiedClientSocket = proxiedClientSocket;
    this.proxyDirector = proxyDirector;

    requestStream = new ProxiedRequestStream(this, (proxiedClientSocket instanceof SSLSocket), proxiedClientSocket.getInputStream(), proxyDirector);
  }

  void proxyRequests()
  {
    try
    {
      while (!closed)
      {
        requestStream.readAndWriteMessage();˙
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
            if (!requestStream.isMessageComplete() || !responseStream.isMessageComplete())
            {
              proxyDirector.onPrematureRequestClosed(requestStream, proxiedIOException.e);
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
          proxyDirector.onRequestHttpProtocolError(requestStream, e.getMessage());
          close();
        }
      }
    }
    catch (EndProxiedRequestException e)
    {
      try
      {
        proxiedClientSocket.getOutputStream().write(e.toString().getBytes());
      }
      catch (IOException ie)
      {}
      close();
    }
  }

  void proxyResponses()
  {
    try
    {
      synchronized (this)
      {
        if (responseStream == null)
        {
          try
          {
            wait();
          }
          catch (InterruptedException e)
          {}
        }
      }

      while (!closed)
      {
        responseStream.readAndWriteMessage();
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
            if (!requestStream.isMessageComplete() || !responseStream.isMessageComplete())
            {
              proxyDirector.onPrematureResponseClosed(requestStream, responseStream, proxiedIOException.e);
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
          proxyDirector.onResponseHttpProtocolError(requestStream, responseStream, e.getMessage());
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
        proxiedClientSocket.getOutputStream().write(e.toString().getBytes());
      }
      catch (IOException ie)
      {}
      close();
    }
  }

  synchronized OutputStream connectProxiedServer() throws IOException, EndProxiedRequestException
  {
    connectingServerSocket = true;
    proxiedServerSocket = proxyDirector.connectToProxiedHost(requestStream);
    if (proxiedServerSocket == null)
    {
      throw EndProxiedRequestException.NOT_FOUND;
    }

    proxiedServerSocket.setKeepAlive(true); // Use keep alives so we know when the far end has shutdown the socket.
    OutputStream outputStream = proxiedServerSocket.getOutputStream();
    connectingServerSocket = false;

    responseStream = new ProxiedResponseStream(this, (proxiedServerSocket instanceof SSLSocket), proxiedServerSocket.getInputStream(), proxiedClientSocket.getOutputStream(), proxyDirector);

    notify();

    return outputStream;
  }

  synchronized void close()
  {
    closed = true;
    if (proxiedClientSocket.isConnected())
    {
      try
      {
        proxiedClientSocket.close();
      }
      catch (Exception e)
      {}
    }

    notify();

    if ((proxiedServerSocket != null) && proxiedServerSocket.isConnected())
    {
      try
      {
        proxiedServerSocket.close();
      }
      catch (Exception e)
      {}
    }
  }
}

package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

class ProxiedExchangeStream
{
  private Socket proxiedClientSocket;

  protected final ProxiedRequestStream requestStream;

  private Socket proxiedServerSocket;

  protected volatile ProxiedResponseStream responseStream;

  private final IOProxyDirector proxyDirector;

  private boolean connectingServerSocket;

  private volatile boolean closed;

  ProxiedExchangeStream(Socket proxiedClientSocket, IOProxyDirector proxyDirector) throws IOException
  {
    this.proxiedClientSocket = proxiedClientSocket;
    this.proxyDirector = proxyDirector;

    requestStream = new ProxiedRequestStream(this, proxiedClientSocket.getInputStream(), proxyDirector);
  }

  void proxyRequests()
  {
    try
    {
      while (!closed)
      {
        requestStream.readAndWriteMessage();
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
  }

  synchronized OutputStream connectProxiedServer() throws IOException
  {
    connectingServerSocket = true;
    proxiedServerSocket = proxyDirector.connectToProxiedHost(requestStream);
    proxiedServerSocket.setKeepAlive(true); // Use keep alives so we know when the far end has shutdown the socket.
    OutputStream outputStream = proxiedServerSocket.getOutputStream();
    connectingServerSocket = false;

    responseStream = new ProxiedResponseStream(this, proxiedServerSocket.getInputStream(), proxiedClientSocket.getOutputStream(), proxyDirector);

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

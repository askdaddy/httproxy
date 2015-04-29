package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 *
 */
public class ServerSocketChannelAcceptLoop
{
  private final SelectorDispatcher selectorDispatcher;

  private volatile boolean started;

  private ServerSocketChannel serverSocketChannel;

  public ServerSocketChannelAcceptLoop(NIOProxyDirector proxyDirector)
  {
    this(proxyDirector, Runtime.getRuntime().availableProcessors());
  }

  public ServerSocketChannelAcceptLoop(NIOProxyDirector proxyDirector, int numSelectorThreads)
  {
    this.selectorDispatcher = new SelectorDispatcher(proxyDirector, numSelectorThreads);
  }

  /**
   * Accepts and dispatches incoming requests on the given ServerSocketChannel. This method blocks the calling thread until
   * {@link #stop()} is called by another thread or the given ServerSocketChannel is no longer bound.
   *
   * @param serverSocketChannel The channel to accept incoming client requests on. The channel must be bound before calling this method.
   * @throws NotYetBoundException If the given ServerSocketChannel is not already bound.
   * @throws SecurityException If the security manager will not allow
   * @throws IOException
   */
  public void start(ServerSocketChannel serverSocketChannel) throws NotYetBoundException, SecurityException, IOException
  {
    try
    {
      started = true;

      if (!selectorDispatcher.isStarted())
      {
        selectorDispatcher.start();
      }

      this.serverSocketChannel = serverSocketChannel;
      while (started)
      {
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (socketChannel != null)
        {
          selectorDispatcher.dispatch(socketChannel);
        }
      }
    }
    catch (IOException e)
    {
      if (started)
      {
        throw e;
      }
    }
    finally
    {
      this.serverSocketChannel = null;
      started = false;
    }
  }

  public void stop()
  {
    started = false;
    if (serverSocketChannel != null)
    {
      try
      {
        serverSocketChannel.close();
      }
      catch (IOException e)
      {}
    }
  }
}

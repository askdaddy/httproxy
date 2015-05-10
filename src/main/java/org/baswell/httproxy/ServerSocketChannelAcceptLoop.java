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
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Accepts and dispatches incoming requests on the given ServerSocketChannel.
 */
public class ServerSocketChannelAcceptLoop
{
  private final SelectorDispatcher selectorDispatcher;

  private volatile boolean started;

  private ServerSocketChannel serverSocketChannel;

  /**
   * Uses {@code Runtime.getRuntime().availableProcessors()} for the number of selector threads (in most circumstances
   * this will be the best option).
   *
   * @param proxyDirector Must be non-null.
   */
  public ServerSocketChannelAcceptLoop(NIOProxyDirector proxyDirector)
  {
    this(proxyDirector, Runtime.getRuntime().availableProcessors());
  }

  /**
   *
   * @param proxyDirector Must be non-null.
   * @param numSelectorThreads The number of selector threads. Requests will be evenly distributed upon each thread.
   */
  public ServerSocketChannelAcceptLoop(NIOProxyDirector proxyDirector, int numSelectorThreads)
  {
    this.selectorDispatcher = new SelectorDispatcher(proxyDirector, numSelectorThreads);
  }

  /**
   * Accepts incoming requests on {@code serverSocketChannel} and dispatches the request to one of the selector threads. This method
   * blocks the calling thread until {@link #stop()} is called by another thread or the given {@code ServerSocketChannel} is no longer bound.
   *
   * @param serverSocketChannel The channel to accept incoming client requests on. The channel must be bound before calling this method.
   * @throws NotYetBoundException If the given ServerSocketChannel is not already bound.
   * @throws SecurityException If a security manager exists and its checkAccept method doesn't allow the operation.
   * @throws IOException If an I/O error occurs when waiting for a connection.
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

  /**
   * Stops the accept loop. The thread blocked on {@link #start(java.nio.channels.ServerSocketChannel)} will be released. No new
   * incoming connections will be made but sockets that have already been accepted will gracefully finish up.
   */
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

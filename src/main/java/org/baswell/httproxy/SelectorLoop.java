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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class SelectorLoop implements Runnable
{
  private final NIOProxyDirector proxyDirector;

  private final ProxyLogger log;

  Selector selector;

  private volatile Thread selectorThread;

  private List<SocketChannel> socketChannelQueue = new ArrayList<SocketChannel>();

  SelectorLoop(NIOProxyDirector proxyDirector)
  {
    this.proxyDirector = proxyDirector;
    this.log = proxyDirector.getLogger();
  }

  void start() throws IOException
  {
    selector = Selector.open();
    selectorThread = new Thread(this, "SelectorLoop-" + THREAD_COUNTER.incrementAndGet());
    selectorThread.start();
  }

  void add(SocketChannel socketChannel)
  {
    socketChannelQueue.add(socketChannel);
    selector.wakeup();
  }

  void stop() throws IOException
  {
    selectorThread = null;
    if (selector != null)
    {
      selector.close();
    }
  }

  @Override
  public void run()
  {
    while (Thread.currentThread() == selectorThread)
    {
      try
      {
        int numberSelected = selector.select();
        if (numberSelected > 0)
        {
          Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
          while (selectionKeys.hasNext())
          {
            SelectionKey selectionKey = selectionKeys.next();
            PipedExchangeChannel connection = (PipedExchangeChannel)selectionKey.attachment();

            if (selectionKey.isValid() && selectionKey.isWritable() && ((selectionKey.interestOps() & SelectionKey.OP_WRITE) != 0))
            {
              connection.onWriteReady(selectionKey);
            }

            if (selectionKey.isValid() && selectionKey.isReadable())
            {
              connection.onReadReady(selectionKey);
            }

            selectionKeys.remove();
          }
        }

        while (!socketChannelQueue.isEmpty())
        {
          try
          {
            new PipedExchangeChannel(this, socketChannelQueue.remove(0), proxyDirector);
          }
          catch (Exception e)
          {
            log.error("Unable to create proxied exchange channel.", e);
          }
        }
      }
      catch (IOException e)
      {
        if (log != null)
        {
          log.error("Selector threw IO error?", e);
        }
      }
    }
  }

  private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
}

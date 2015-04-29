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

  Selector selector;

  private volatile Thread selectorThread;

  private List<SocketChannel> socketChannelQueue = new ArrayList<SocketChannel>();

  SelectorLoop(NIOProxyDirector proxyDirector)
  {
    this.proxyDirector = proxyDirector;
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
            ProxiedExchangeChannel connection = (ProxiedExchangeChannel)selectionKey.attachment();

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
            new ProxiedExchangeChannel(this, socketChannelQueue.remove(0), proxyDirector);
          }
          catch (Exception e)
          {}
        }
      }
      catch (IOException e)
      {
       // TODO Selector threw IO error so what does that mean and what to do ?
        e.printStackTrace();
      }
    }
  }

  private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
}

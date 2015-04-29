package org.baswell.httproxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

class SelectorDispatcher
{
  private final NIOProxyDirector proxyDirector;

  private final int numSelectorThreads;

  private int selectorIndex;

  private List<SelectorLoop> selectorLoops;

  SelectorDispatcher(NIOProxyDirector proxyDirector, int numSelectorThreads)
  {
    this.proxyDirector = proxyDirector;
    this.numSelectorThreads = numSelectorThreads;
  }

  synchronized boolean isStarted()
  {
    return (selectorLoops != null) && !selectorLoops.isEmpty();
  }

  synchronized void start() throws IOException
  {
    stop();
    selectorLoops = new ArrayList<SelectorLoop>();
    for (int i = 0; i < numSelectorThreads; i++)
    {
      SelectorLoop selectorLoop = new SelectorLoop(proxyDirector);
      selectorLoop.start();

      selectorLoops.add(selectorLoop);
    }
  }

  synchronized void stop()
  {
    if (selectorLoops != null)
    {
      for (SelectorLoop selectorLoop : selectorLoops)
      {
        try
        {
          selectorLoop.stop();
        }
        catch (IOException e)
        {}
      }
      selectorLoops = null;
    }
  }

  synchronized void dispatch(SocketChannel socketChannel) throws IOException
  {
    ++selectorIndex;
    if (selectorIndex < 0)
    {
      selectorIndex = 0;
    }
    selectorLoops.get(selectorIndex % selectorLoops.size()).add(socketChannel);
  }
}

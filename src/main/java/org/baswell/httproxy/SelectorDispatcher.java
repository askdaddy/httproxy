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

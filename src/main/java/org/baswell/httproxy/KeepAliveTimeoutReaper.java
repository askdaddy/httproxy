package org.baswell.httproxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class KeepAliveTimeoutReaper implements Runnable
{
  private final IOProxyDirector proxyDirector;

  private final List<ReapedPipedExchange> exchanges = Collections.synchronizedList(new ArrayList<ReapedPipedExchange>());

  private Thread thread;

  KeepAliveTimeoutReaper(IOProxyDirector proxyDirector)
  {
    this.proxyDirector = proxyDirector;

    thread = new Thread(this, KeepAliveTimeoutReaper.class.getSimpleName());
    thread.start();
  }

  synchronized void watch(ReapedPipedExchange exchange)
  {
    exchanges.add(exchange);
    notify();
  }

  @Override
  public void run()
  {
    while (thread == Thread.currentThread())
    {
      int sleepSeconds = proxyDirector.getKeepAliveSleepSeconds();
      for (int i = exchanges.size() - 1; i >= 0; i--)
      {
        ReapedPipedExchange exchange = exchanges.get(i);
        if (exchange.closed())
        {
          exchanges.remove(i);
        }
        else if (!exchange.active())
        {
          Integer timeoutSecs = exchange.getLastKeepAliveTimeoutSeconds();
          if (timeoutSecs != null)
          {
            int secondsSinceLastExchange = (int)((System.currentTimeMillis() - exchange.getLastExchangeAt()) / 1000);
            if (secondsSinceLastExchange >= timeoutSecs)
            {
              exchange.close();
              exchanges.remove(i);
            }
            else
            {
              sleepSeconds = Math.min(sleepSeconds, (timeoutSecs - secondsSinceLastExchange));
            }
          }
        }
      }


      if (!exchanges.isEmpty())
      {
        try
        {
          Thread.sleep(sleepSeconds * 1000);
        }
        catch (Exception e)
        {}
      }
      else
      {
        synchronized (this)
        {
          if (exchanges.isEmpty())
          {
            try
            {
              wait();
            }
            catch (InterruptedException e)
            {}
          }
        }
      }
    }
  }
}

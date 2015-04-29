package org.baswell.httproxy;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

class ThreadPoolDispatcher
{
  private final ExecutorService executorService;

  private final IOProxyDirector proxyDirector;

  ThreadPoolDispatcher(IOProxyDirector proxyDirector, ExecutorService executorService)
  {
    this.proxyDirector = proxyDirector;
    this.executorService = executorService;
  }

  void dispatch(Socket socket)
  {
    try
    {
      final ProxiedExchangeStream exchangeStream = new ProxiedExchangeStream(socket, proxyDirector);

      executorService.execute(new Runnable()
      {
        public void run()
        {
          exchangeStream.proxyRequests();
        }
      });

      executorService.execute(new Runnable()
      {
        public void run()
        {
          exchangeStream.proxyResponses();
        }
      });

    }
    catch (IOException e)
    {}

  }
}

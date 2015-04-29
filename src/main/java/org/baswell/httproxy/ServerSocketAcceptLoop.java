package org.baswell.httproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NotYetBoundException;
import java.util.concurrent.ExecutorService;

public class ServerSocketAcceptLoop
{
  private final ThreadPoolDispatcher threadPoolDispatcher;

  private volatile boolean started;

  private ServerSocket serverSocket;

  public ServerSocketAcceptLoop(IOProxyDirector proxyDirector, ExecutorService executorService)
  {
    this.threadPoolDispatcher = new ThreadPoolDispatcher(proxyDirector, executorService);
  }

  public void start(ServerSocket serverSocket) throws NotYetBoundException, SecurityException, IOException
  {
    try
    {
      started = true;
      this.serverSocket = serverSocket;
      while (started)
      {
        Socket socket = serverSocket.accept();
        if (socket != null)
        {
          threadPoolDispatcher.dispatch(socket);
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
      this.serverSocket = null;
      started = false;
    }
  }

  public void stop()
  {
    started = false;
    if (serverSocket != null)
    {
      try
      {
        serverSocket.close();
      }
      catch (IOException e)
      {}
    }
  }
}

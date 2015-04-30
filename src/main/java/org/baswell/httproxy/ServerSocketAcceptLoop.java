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

  /**
   * Accepts incoming requests on {@code serverSocket} and dispatches using the {@code ExecutorService} passed in from the constructor. This method
   * blocks the calling thread until {@link #stop()} is called by another thread or the given {@code ServerSocket} is no longer bound.
   *
   * @param serverSocket The server socket to accept incoming client requests on. This socket must be bound before calling this method.
   * @throws NotYetBoundException If the given {@code ServerSocket} is not already bound.
   * @throws SecurityException If a security manager exists and its checkAccept method doesn't allow the operation.
   * @throws IOException If an I/O error occurs when waiting for a connection.
   */
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

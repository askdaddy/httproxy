package org.baswell.httproxy.servers;

import org.baswell.httproxy.ServerSocketAcceptLoop;
import org.baswell.httproxy.SimpleIODirector;

import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IOServer
{
  public static void main(String[] args) throws Exception
  {
    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(250, 2000, 25, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    ServerSocket serverSocket = new ServerSocket(9090);
    ServerSocketAcceptLoop acceptLoop = new ServerSocketAcceptLoop(new SimpleIODirector("localhost", 8080), threadPool);
    acceptLoop.start(serverSocket);
  }
}

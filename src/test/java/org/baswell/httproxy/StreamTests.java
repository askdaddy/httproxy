package org.baswell.httproxy;

import com.sun.net.httpserver.HttpContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class StreamTests extends ProxyTests
{
  static TestIOProxyDirector proxyDirector = new TestIOProxyDirector();

  static ServerSocket serverSocket;

  static ServerSocketAcceptLoop socketAcceptLoop;

  @BeforeClass
  public static void beforeAllTests() throws Exception
  {
    startServer();

    serverSocket = new ServerSocket(8081);
    socketAcceptLoop = new ServerSocketAcceptLoop(proxyDirector, new ThreadPoolExecutor(10, 100, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));
    Thread acceptThread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          socketAcceptLoop.start(serverSocket);
        }
        catch (IOException e)
        {
          fail(e.getMessage());
        }
      }
    });
    acceptThread.start();

    Thread.sleep(500);
  }

  @Before
  public void beforeEachTest()
  {
    proxyDirector.reset();
  }

  @AfterClass
  public static void afterAllTests()
  {
    stopServer();
    if (socketAcceptLoop != null) socketAcceptLoop.stop();
  }


  @Test
  public void testOne() throws Exception
  {
    httpHandler = new HttpHandler()
    {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException
      {
        String response = "HELLO WORLD!";
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream out = httpExchange.getResponseBody();
        out.write(response.getBytes());
        out.close();
      }
    };

    HttpRequest request = new HttpRequest();
    HttpResponse response = sendRequest(request);

    assertEquals(200, response.status);
    assertEquals("HELLO WORLD!", response.content);

    assertEquals("/test", proxyDirector.requestPath);
    assertEquals("localhost:8081", proxyDirector.host);
    assertEquals(200, proxyDirector.responseCode);
  }
}

package org.baswell.httproxy;

import com.sun.net.httpserver.HttpContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.Arrays;
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

    serverSocket = new ServerSocket(9095);
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
  public void testBasicRequest() throws Exception
  {
    httpHandler = new HttpHandler()
    {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException
      {
        String response = "HELLO WORLD!";
        httpExchange.getResponseHeaders().put("Three", Arrays.asList("Four"));
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream out = httpExchange.getResponseBody();
        out.write(response.getBytes());
        out.close();
      }
    };

    HttpRequest request = new HttpRequest();
    request.headers.put("One", "Two");
    HttpResponse response = sendRequest(request);

    assertEquals(200, response.status);
    assertEquals("HELLO WORLD!", new String(response.content));

    assertEquals("/test", proxyDirector.requestPath);
    assertEquals("localhost:9095", proxyDirector.host);
    assertTrue(proxyDirector.requestHeaders.containsKey("One"));
    assertEquals("Two", proxyDirector.requestHeaders.get("One"));
    assertEquals(200, proxyDirector.responseCode);
    assertTrue(proxyDirector.responseHeaders.containsKey("Three"));
    assertEquals("Four", proxyDirector.responseHeaders.get("Three"));
  }

  @Test
  public void testChangeHeaders() throws Exception
  {
    httpHandler = new HttpHandler()
    {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException
      {
        assertTrue(httpExchange.getRequestHeaders().containsKey("One"));
        assertEquals("2", httpExchange.getRequestHeaders().get("One").get(0));

        String content = "HELLO WORLD!";
        httpExchange.getResponseHeaders().put("Three", Arrays.asList("Four"));
        httpExchange.sendResponseHeaders(200, content.length());
        OutputStream out = httpExchange.getResponseBody();
        out.write(content.getBytes());
        out.close();
      }
    };

    proxyDirector.modifiedRequestHeaders.put("One", "2");
    proxyDirector.modifiedResponseHeaders.put("Three", "4");

    HttpRequest request = new HttpRequest();
    request.headers.put("One", "Two");

    HttpResponse response = sendRequest(request);

    assertTrue(response.headers.containsKey("Three"));
    assertEquals("4", response.headers.get("Three"));
  }

  @Test
  public void testChunkedEncoding() throws Exception
  {
    httpHandler = new HttpHandler()
    {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException
      {
        httpExchange.sendResponseHeaders(200, 0);
        stream(StreamTests.class.getResourceAsStream("/pic.png"), httpExchange.getResponseBody());
      }
    };

    HttpRequest request = new HttpRequest();
    HttpResponse response = sendRequest(request);

    assertTrue(proxyDirector.responseHeaders.containsKey("Transfer-encoding"));
    assertEquals("chunked", proxyDirector.responseHeaders.get("Transfer-encoding"));
    assertEquals(2983425, response.content.length);
  }
}

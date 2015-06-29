package org.baswell.httproxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ChannelTests extends ProxyTests
{
  static TestNIOProxyDirector proxyDirector = new TestNIOProxyDirector();

  static ServerSocketChannel serverSocket;

  static ServerSocketChannelAcceptLoop socketAcceptLoop;


  @BeforeClass
  public static void beforeAllTests() throws Exception
  {
    startServer();

    serverSocket = ServerSocketChannel.open();
    serverSocket.socket().bind(new InetSocketAddress(9095));

    socketAcceptLoop = new ServerSocketChannelAcceptLoop(proxyDirector);
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

    TestHttpRequest request = new TestHttpRequest();
    request.headers.put("One", "Two");
    TestHttpResponse response = sendRequest(request);

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

    TestHttpRequest request = new TestHttpRequest();
    request.headers.put("One", "Two");

    TestHttpResponse response = sendRequest(request);

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

    TestHttpRequest request = new TestHttpRequest();
    TestHttpResponse response = sendRequest(request);

    assertTrue(proxyDirector.responseHeaders.containsKey("Transfer-encoding"));
    assertEquals("chunked", proxyDirector.responseHeaders.get("Transfer-encoding"));
    assertEquals(2983425, response.content.length);
  }
}

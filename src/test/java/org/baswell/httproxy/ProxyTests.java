package org.baswell.httproxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;

abstract public class ProxyTests
{
  protected static HttpServer server;

  protected static HttpHandler httpHandler;

  public static void startServer() throws Exception
  {
    server = HttpServer.create(new InetSocketAddress(9096), 0);
    server.createContext("/test", new HttpHandler()
    {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException
      {
        httpHandler.handle(httpExchange);
      }
    });
    server.start();

  }

  static public void stopServer()
  {
    try
    {
      server.stop(0);
    }
    catch (Exception e)
    {}
  }

  static HttpResponse sendRequest(HttpRequest request) throws IOException
  {
    URL url = new URL(request.url);
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    urlConnection.setRequestMethod(request.method);
    for (Map.Entry<String, String> entry : request.headers.entrySet())
    {
      urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
    }

    if (request.content != null)
    {
      urlConnection.setDoOutput(true);
      urlConnection.getOutputStream().write(request.content.getBytes());
    }

    HttpResponse response = new HttpResponse();

    response.status = urlConnection.getResponseCode();
    response.reason = urlConnection.getResponseMessage();
    response.content = getContent(urlConnection.getInputStream());

    for (Map.Entry<String, List<String>> entry : urlConnection.getHeaderFields().entrySet())
    {
      response.headers.put(entry.getKey(), entry.getValue().get(0));
    }

    return response;
  }

  static byte[] getContent(InputStream inputStream) throws IOException
  {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int read;

    while ((read = inputStream.read(buffer)) != -1)
    {
      bytesOut.write(buffer, 0, read);
    }

    return bytesOut.toByteArray();
  }

  static void stream(InputStream inputStream, OutputStream outputStream) throws IOException
  {
    byte[] buffer = new byte[1024 * 1024];
    int read;

    while ((read = inputStream.read(buffer)) != -1)
    {
      outputStream.write(buffer, 0, read);
    }

    outputStream.flush();
    outputStream.close();
    inputStream.close();
  }


}

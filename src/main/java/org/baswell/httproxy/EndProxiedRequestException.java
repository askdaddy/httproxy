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

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * When thrown from {@link NIOProxyDirector#connectToProxiedHost(ProxiedRequest)} or {@link IOProxyDirector#connectToProxiedHost(ProxiedRequest)} methods the response code
 * will be returned to the client and the connection closed.
 * </p>
 */
public class EndProxiedRequestException extends Exception
{
  /**
   * <p>
   * Shortcut for returning 200.
   * </p>
   *
   * <pre>
   * throw ReturnHttpResponseStatus.OK;
   * </pre>
   */
  public static EndProxiedRequestException OK = new EndProxiedRequestException(200, "Ok");


  /**
   * <p>
   * Shortcut for returning 400.
   * </p>
   *
   * <pre>
   * throw ReturnHttpResponseStatus.BAD_REQUEST;
   * </pre>
   */
  public static EndProxiedRequestException BAD_REQUEST = new EndProxiedRequestException(400, "Bad Request");

  /**
   * <p>
   * Shortcut for returning 403.
   * </p>
   *
   * <pre>
   * throw ReturnHttpResponseStatus.FORBIDDEN;
   * </pre>
   */
  public static EndProxiedRequestException FORBIDDEN = new EndProxiedRequestException(403, "Forbidden");

  /**
   * <p>
   * Shortcut for returning 404.
   * </p>
   *
   * <pre>
   * throw ReturnHttpResponseStatus.NOT_FOUND;
   * </pre>
   */
  public static EndProxiedRequestException NOT_FOUND = new EndProxiedRequestException(404, "Not Found");

  /**
   * <p>
   * Shortcut for returning 500.
   * </p>
   *
   * <pre>
   * throw ReturnHttpResponseStatus.INTERNAL_SERVER_ERROR;
   * </pre>
   */
  public static EndProxiedRequestException INTERNAL_SERVER_ERROR = new EndProxiedRequestException(500, "Internal Server Error");

  /**
   * <p>Shortcut for returning redirect 301 to the given uri.</p>
   *
   * <pre>
   * throw ReturnHttpResponseStatus.redirectPermanently("https://test.org/helloworld");
   * </pre>
   */
  public static EndProxiedRequestException redirectPermanently(String uri)
  {
    return new EndProxiedRequestException(301, "Moved Permanently", Arrays.asList(new Header("Location", uri)));
  }

  /**
   * The HTTP status to return.
   */
  public final int code;

  public final String message;

  public final List<Header> headers;

  /**
   *
   * <pre>
   * HTTP/1.0 code message
   * </pre>
   *
   * @param code The HTTP status code to return.
   * @param message The HTTP status message to return.
   */
  public EndProxiedRequestException(int code, String message)
  {
    this(code, message, null);
  }

  /**
   *
   * <pre>
   * HTTP/1.0 code message
   * </pre>
   *
   * @param code The HTTP status code to return.
   * @param message The HTTP status message to return.
   * @param headers The HTTP headers to send back in the response.
   */
  public EndProxiedRequestException(int code, String message, List<Header> headers)
  {
    this.code = code;
    this.message = message;
    this.headers = headers;
  }

  @Override
  public String toString()
  {
    String http = "HTTP/1.1 " + code + " " + message + ProxiedMessage.CRLF + "Connection: close" + ProxiedMessage.CRLF;
    if (headers != null)
    {
      for (Header header : headers)
      {
        http += header.name + ": " + header.value + ProxiedMessage.CRLF;
      }
    }

    http += ProxiedMessage.CRLF;
    return http;
  }
}

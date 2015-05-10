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

/**
 * Details about a proxied client request.
 */
public interface ProxiedRequest
{
  /**
   *
   * @return When the first byte of this request was received.
   */
  long startedAt();

  /**
   *
   * @return When the last byte of this request was received.
   */
  long endedAt();

  /**
   * Host: <b>www.host1.com:80</b>
   *
   * @return The <i>Host</i> header value for this request.
   */
  String host();

  /**
   * <b>GET</b> /path/file.html HTTP/1.1
   *
   * @return The HTTP method.
   */
  String method();

  /**
   *
   * GET <b>/path/file.html</b> HTTP/1.1
   *
   * @return The HTTP request path.
   */
  String path();

  /**
   * Attach an arbitrary object to this request. The attached object will stay with this proxied request for the life time of the
   * connection (if multiple requests are made with the same socket connection the attachment will survive across the multiple
   * requests).
   *
   * @param attachment
   * @see #attachment()
   */
  void attach(Object attachment);

  /**
   *
   * @return The attached object of this request.
   * @see #attach(Object)
   */
  Object attachment();
}

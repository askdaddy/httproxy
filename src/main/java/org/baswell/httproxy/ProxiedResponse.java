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
 * Details about a proxied server response.
 */
public interface ProxiedResponse
{
  /**
   *
   * @return When the first byte of this response was received.
   */
  long startedAt();

  /**
   *
   * @return When the last byte of this response was received.
   */
  long endedAt();

  /**
   * HTTP/1.0 <b>404</b> Not Found
   *
   * @return The HTTP status code.
   */
  int status();

  /**
   * HTTP/1.0 404 <b>Not Found</b>
   *
   * @return The HTTP status reason.
   */
  String reason();

  /**
   * Attach an arbitrary object to this response. The attached object will stay with this proxied request for the life time of the
   * connection (if multiple responses are made with the same socket connection the attachment will survive across each
   * responses).
   *
   * @param attachment
   * @see #attachment()
   */
  void attach(Object attachment);

  /**
   *
   * @return The attached object of this response.
   * @see #attach(Object)
   */
  Object attachment();
}

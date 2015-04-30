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

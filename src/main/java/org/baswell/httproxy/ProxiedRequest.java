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

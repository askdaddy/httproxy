package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An API for modifying the content from the proxied server sent back to the client.
 *
 * @see IOProxyDirector#getResponseModifier(HttpRequest, HttpResponse)
 */
public interface ResponseContentModifier
{
  /**
   * Called multiple times as content from proxied server is received. As content is modified it should be sent back to the
   * client using the given outputStream (ex. {@link OutputStream#write(byte[])}. The passed in bytes can be buffered as
   * needed as long as all buffered content is written when {@link #responseComplete(HttpRequest, HttpResponse, OutputStream)} is called.
   *
   * @param  httpRequest The HTTP request.
   * @param  httpResponse The HTTP response.
   * @param bytes Response bytes from the proxied server for this HTTP response.
   * @param outputStream The output stream to the client.
   * @throws IOException If output to client fails.
   */
  void modifyAndWrite(HttpRequest httpRequest, HttpResponse httpResponse, byte[] bytes, OutputStream outputStream) throws IOException;

  /**
   * Notification that all bytes from the proxied server has been received. All buffered content must be written to the
   * outputStream when this method is called.
   *
   * @param  httpRequest The HTTP request.
   * @param  httpResponse The HTTP response.
   * @param outputStream The output stream to the client.
   * @throws IOException If output to client fails.
   */
  void responseComplete(HttpRequest httpRequest, HttpResponse httpResponse, OutputStream outputStream) throws IOException;
}

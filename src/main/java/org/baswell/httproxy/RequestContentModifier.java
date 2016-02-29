package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * An API for modifying the content from the proxied server sent back to the client. The content encoding and transfer encoding
 * are taken care for you. The bytes you are given and write from this API are the application level bytes (i.e. HTML, CSS, etc.).
 *
 * @see IOProxyDirector#getResponseModifier(HttpRequest, HttpResponse)
 */
public interface RequestContentModifier extends ContentModifier
{
  /**
   * Called multiple times as content from client is received. As content is modified it should be sent to the
   * proxied server using the given outputStream (ex. {@link OutputStream#write(byte[])}. The passed in bytes can be buffered as
   * needed as long as all buffered content is written when {@link #responseComplete(HttpRequest, HttpResponse, Charset, OutputStream)} is called.
   *
   * @param httpRequest The HTTP request.
   * @param bytes Response bytes from the proxied server for this HTTP response.
   * @param charset The charset of the given bytes or <code>null</code> if not known.
   * @param outputStream The output stream to the client.
   * @throws IOException If output to client fails.
   */
  void modifyAndWrite(HttpRequest httpRequest, byte[] bytes, Charset charset, OutputStream outputStream) throws IOException;

  /**
   * Notification that all bytes from the proxied server has been received. All buffered content must be written to the
   * outputStream when this method is called.
   *
   * @param httpRequest The HTTP request.
   * @param charset The charset of the previously given bytes for this httpRequest or <code>null</code> if not known.
   * @param outputStream The output stream to the client.
   * @throws IOException If output to client fails.
   */
  void requestComplete(HttpRequest httpRequest, Charset charset, OutputStream outputStream) throws IOException;
}

package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

class PassThroughModifier implements ResponseContentModifier
{
  @Override
  public void modifyAndWrite(HttpRequest httpRequest, HttpResponse httpResponse, byte[] bytes, Charset charset, OutputStream outputStream) throws IOException
  {
    outputStream.write(bytes);
  }

  @Override
  public void responseComplete(HttpRequest httpRequest, HttpResponse httpResponse, Charset charset, OutputStream outputStream) throws IOException
  {}
}

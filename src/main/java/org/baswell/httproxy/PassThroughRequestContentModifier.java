package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

class PassThroughRequestContentModifier implements RequestContentModifier
{
  @Override
  public void modifyAndWrite(HttpRequest httpRequest, byte[] bytes, Charset charset, OutputStream outputStream) throws IOException
  {
    outputStream.write(bytes);
  }

  @Override
  public void requestComplete(HttpRequest httpRequest, Charset charset, OutputStream outputStream) throws IOException
  {}
}

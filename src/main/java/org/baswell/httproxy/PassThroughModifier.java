package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;

class PassThroughModifier implements ResponseContentModifier
{
  @Override
  public void modifyAndWrite(HttpRequest httpRequest, HttpResponse httpResponse, byte[] bytes, OutputStream outputStream) throws IOException
  {
    outputStream.write(bytes);
  }

  @Override
  public void responseComplete(HttpRequest httpRequest, HttpResponse httpResponse, OutputStream outputStream) throws IOException
  {}
}

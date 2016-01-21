package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;

class PassThroughModifier implements ResponseContentModifier
{
  @Override
  public void modifyAndWrite(byte[] bytes, OutputStream outputStream) throws IOException
  {
    outputStream.write(bytes);
  }

  @Override
  public void responseComplete(OutputStream outputStream) throws IOException
  {}
}

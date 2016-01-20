package org.baswell.httproxy;

import java.io.IOException;

class PassThroughModifier implements ResponseContentModifier
{
  @Override
  public void modifyAndWrite(byte[] bytes, ModifiedOutput modifiedOutput) throws IOException
  {
    modifiedOutput.write(bytes);
  }

  @Override
  public void responseComplete(ModifiedOutput modifiedOutput) throws IOException
  {}
}

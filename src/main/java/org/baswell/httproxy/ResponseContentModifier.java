package org.baswell.httproxy;

import java.io.IOException;

/**
 *
 */
public interface ResponseContentModifier
{
  void modifyAndWrite(byte[] bytes, ModifiedOutput modifiedOutput) throws IOException;

  void responseComplete(ModifiedOutput modifiedOutput) throws IOException;
}

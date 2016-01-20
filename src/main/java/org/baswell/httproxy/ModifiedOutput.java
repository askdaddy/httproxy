package org.baswell.httproxy;

import java.io.IOException;

public interface ModifiedOutput
{
  void write(byte[] bytes) throws IOException;
}

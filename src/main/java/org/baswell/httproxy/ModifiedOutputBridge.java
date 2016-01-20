package org.baswell.httproxy;

import java.io.IOException;

public interface ModifiedOutputBridge
{
  void push(byte[] bytes) throws IOException;
}

package org.baswell.httproxy;

import java.io.IOException;

public interface ResponseContentModifier
{
  void push(byte[] bytes, ModifiedOutputBridge bridge) throws IOException;

  void complete(ModifiedOutputBridge bridge) throws IOException;
}

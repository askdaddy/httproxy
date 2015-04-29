package org.baswell.httproxy;

import java.io.IOException;
import java.net.Socket;

public class SimpleIODirector extends SimpleProxyDirector implements IOProxyDirector
{
  public SimpleIODirector(String proxiedHost, int proxiedPort)
  {
    super(proxiedHost, proxiedPort);
  }

  @Override
  public int getSleepSecondsOnReadWait()
  {
    return 5;
  }

  @Override
  public Socket connectToProxiedHost(ProxiedRequest request) throws IOException
  {
    return new Socket(proxiedHost, proxiedPort);
  }
}

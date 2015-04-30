package org.baswell.httproxy;

import java.io.IOException;
import java.net.Socket;

/**
 * A simple IOProxyDirector that proxies to a single server and prints out proxy events.
 */
public class SimpleIODirector extends SimpleProxyDirector implements IOProxyDirector
{
  /**
   * @see #getSleepSecondsOnReadWait()
   */
  public int sleepSecondsOnReadWait = 5;

  public SimpleIODirector(String proxiedHost, int proxiedPort)
  {
    super(proxiedHost, proxiedPort);
  }

  @Override
  public int getSleepSecondsOnReadWait()
  {
    return sleepSecondsOnReadWait;
  }

  @Override
  public Socket connectToProxiedHost(ProxiedRequest request) throws IOException
  {
    return new Socket(proxiedHost, proxiedPort);
  }
}

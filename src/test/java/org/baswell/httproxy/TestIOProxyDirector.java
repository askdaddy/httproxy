package org.baswell.httproxy;

import java.io.IOException;
import java.net.Socket;

public class TestIOProxyDirector extends TestProxyDirector implements IOProxyDirector
{
  public String serverHost = "localhost";

  public int serverPort = 9096;

  public int sleepSecondsOnReadWait = 1;

  @Override
  public int getSleepSecondsOnReadWait()
  {
    return sleepSecondsOnReadWait;
  }

  @Override
  public Socket connectToProxiedHost(ProxiedRequest request) throws IOException
  {
    return new Socket(serverHost, serverPort);
  }
}

/*
 * Copyright 2015 Corey Baswell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

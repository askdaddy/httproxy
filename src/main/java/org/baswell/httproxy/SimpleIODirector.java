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

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * A simple IOProxyDirector that proxies to a single server and prints out proxy events.
 */
public class SimpleIODirector extends SimpleProxyDirector implements IOProxyDirector
{
  /**
   * @see #getSleepSecondsOnReadWait()
   */
  public int sleepSecondsOnReadWait = 5;

  private final ExecutorService ioThreadPool;

  public SimpleIODirector(String proxiedHost, int proxiedPort, ExecutorService ioThreadPool)
  {
    super(proxiedHost, proxiedPort);
    this.ioThreadPool = ioThreadPool;
  }

  public SimpleIODirector(String proxiedHost, int proxiedPort, SSLContext sslContext, ExecutorService ioThreadPool)
  {
    super(proxiedHost, proxiedPort, sslContext);
    this.ioThreadPool = ioThreadPool;
  }

  @Override
  public int getSleepSecondsOnReadWait()
  {
    return sleepSecondsOnReadWait;
  }

  @Override
  public ExecutorService getIOThreadPool()
  {
    return ioThreadPool;
  }
}

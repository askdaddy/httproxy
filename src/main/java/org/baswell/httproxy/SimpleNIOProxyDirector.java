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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A simple NIOProxyDirector that proxies to a single server and prints out proxy events.
 */
public class SimpleNIOProxyDirector extends SimpleProxyDirector implements NIOProxyDirector
{
  /**
   * @see #getMaxWriteAttempts()
   */
  public int maxWriteAttempts = 5;

  private final ExecutorService sslThreadPool;

  public SimpleNIOProxyDirector(String proxiedHost, int proxiedPort)
  {
    super(proxiedHost, proxiedPort);
    sslThreadPool = null;
  }

  public SimpleNIOProxyDirector(String proxiedHost, int proxiedPort, SSLContext sslContext, ExecutorService sslThreadPool)
  {
    super(proxiedHost, proxiedPort, sslContext);
    this.sslThreadPool = sslThreadPool;
  }

  @Override
  public int getMaxWriteAttempts()
  {
    return maxWriteAttempts;
  }

  @Override
  public ExecutorService getSSLThreadPool()
  {
    return sslThreadPool;
  }
}

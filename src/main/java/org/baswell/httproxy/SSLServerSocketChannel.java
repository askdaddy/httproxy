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
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>A wrapper around a real {@link ServerSocketChannel} that produces {@link SSLSocketChannel} on {@link #accept()}. The real ServerSocketChannel must be
 * bound externally (to this class) before calling the accept method.</p>
 *
 * @see SSLSocketChannel
 */
class SSLServerSocketChannel extends ServerSocketChannel
{
  /**
   * Should the SSLSocketChannels created from the accept method be put in blocking mode. Default is {@code false}.
   */
  public boolean blockingMode;

  /**
   * Should the SS server ask for client certificate authentication? Default is {@code false}.
   */
  public boolean wantClientAuthentication;

  /**
   * Should the SSL server require client certificate authentication? Default is {@code false}.
   */
  public boolean needClientAuthentication;

  /**
   * The list of SSL protocols (TLSv1, TLSv1.1, etc.) supported for the SSL exchange. Default is the JVM default.
   */
  public List<String> includedProtocols;

  /**
   * A list of SSL protocols (SSLv2, SSLv3, etc.) to explicitly exclude for the SSL exchange. Default is none.
   */
  public List<String> excludedProtocols;

  /**
   * The list of ciphers allowed for the SSL exchange. Default is the JVM default.
   */
  public List<String> includedCipherSuites;

  /**
   * A list of ciphers to explicitly exclude for the SSL exchange. Default is none.
   */
  public List<String> excludedCipherSuites;

  private final ServerSocketChannel serverSocketChannel;

  private final SSLContext sslContext;

  private final NIOProxyDirector proxyDirector;

  /**
   *
   * @param serverSocketChannel The real server socket channel that accepts network requests.
   * @param sslContext The SSL context used to create the {@link SSLEngine} for incoming requests.
   */
  public SSLServerSocketChannel(ServerSocketChannel serverSocketChannel, SSLContext sslContext, NIOProxyDirector proxyDirector)
  {
    super(serverSocketChannel.provider());
    this.serverSocketChannel = serverSocketChannel;
    this.sslContext = sslContext;
    this.proxyDirector = proxyDirector;
  }

  @Override
  public ServerSocket socket()
  {
    return serverSocketChannel.socket();
  }

  @Override
  public SocketChannel accept() throws IOException
  {
    SocketChannel channel = serverSocketChannel.accept();
    channel.configureBlocking(blockingMode);

    SSLEngine sslEngine = sslContext.createSSLEngine();
    sslEngine.setUseClientMode(false);
    sslEngine.setWantClientAuth(wantClientAuthentication);
    sslEngine.setNeedClientAuth(needClientAuthentication);
    sslEngine.setEnabledProtocols(filterArray(sslEngine.getEnabledProtocols(), includedProtocols, excludedProtocols));
    sslEngine.setEnabledCipherSuites(filterArray(sslEngine.getEnabledCipherSuites(), includedCipherSuites, excludedCipherSuites));

    return new SSLSocketChannel(channel, sslEngine, proxyDirector.getSSLThreadPool(), proxyDirector.getLogger());
  }

  @Override
  protected void implCloseSelectableChannel() throws IOException
  {
    serverSocketChannel.close();
  }

  @Override
  protected void implConfigureBlocking(boolean b) throws IOException
  {
    serverSocketChannel.configureBlocking(b);
  }

  static String[] filterArray(String[] items, List<String> includedItems, List<String> excludedItems)
  {
    List<String> filteredItems = new ArrayList<String>(Arrays.asList(items));
    if (includedItems != null)
    {
      for (int i = filteredItems.size() - 1; i >= 0; i--)
      {
        if (!includedItems.contains(filteredItems.get(i)))
        {
          filteredItems.remove(i);
        }
      }

      for (String includedProtocol : includedItems)
      {
        if (!filteredItems.contains(includedProtocol))
        {
          filteredItems.add(includedProtocol);
        }
      }
    }

    if (excludedItems != null)
    {
      for (int i = filteredItems.size() - 1; i >= 0; i--)
      {
        if (excludedItems.contains(filteredItems.get(i)))
        {
          filteredItems.remove(i);
        }
      }
    }

    return filteredItems.toArray(new String[filteredItems.size()]);
  }
}

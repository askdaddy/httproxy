package org.baswell.httproxy;

import sun.security.ntlm.Server;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class SSLServerSocketChannel extends ServerSocketChannel
{
  public boolean wantClientAuthentication;

  public boolean needClientAuthentication;

  private final ServerSocketChannel serverSocketChannel;

  private final SSLContext sslContext;

  private final ExecutorService threadPool;

  public SSLServerSocketChannel(ServerSocketChannel serverSocketChannel, SSLContext sslContext, ExecutorService threadPool)
  {
    super(serverSocketChannel.provider());
    this.serverSocketChannel = serverSocketChannel;
    this.sslContext = sslContext;
    this.threadPool = threadPool;
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
    if (channel == null)
    {
      return null;
    }
    else
    {
      SSLEngine sslEngine = sslContext.createSSLEngine();
      sslEngine.setUseClientMode(false);
      sslEngine.setWantClientAuth(wantClientAuthentication);
      sslEngine.setNeedClientAuth(needClientAuthentication);
      return new ProxiedSSLSocketChannel(channel, sslEngine, threadPool);
    }
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
}

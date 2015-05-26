package org.baswell.httproxy;

/*
http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
 */
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class SSLSocketChannel extends SocketChannel implements WrappedSocketChannel
{
  static public SSLSocketChannel create(InetSocketAddress address, SSLContext sslContext, ExecutorService executorService) throws IOException
  {
    SocketChannel socketChannel = SocketChannel.open(address);
    socketChannel.configureBlocking(false);
    SSLEngine sslEngine = sslContext.createSSLEngine(address.getHostName(), address.getPort());
    sslEngine.setUseClientMode(true);

    return new SSLSocketChannel(socketChannel, sslEngine, executorService);
  }

  private final SocketChannel socketChannel;

  private final SSLEngine sslEngine;

  private final ExecutorService executorService;

  private final ByteBuffer applicationInboundBuffer;

  private final ByteBuffer networkInboundBuffer;

  private boolean handShakeComplete;

  public SSLSocketChannel(SocketChannel socketChannel, SSLEngine sslEngine, ExecutorService executorService) throws IOException
  {
    super(socketChannel.provider());
    this.socketChannel = socketChannel;
    this.sslEngine = sslEngine;
    this.executorService = executorService;

    SSLSession session = sslEngine.getSession();
    applicationInboundBuffer = ByteBuffer.allocate(session.getApplicationBufferSize());
    networkInboundBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
  }

    @Override
  public SocketChannel unwrap()
  {
    return socketChannel;
  }

  @Override
  public Socket socket()
  {
    return null;
  }

  @Override
  public boolean isConnected()
  {
    return false;
  }

  @Override
  public boolean isConnectionPending()
  {
    return false;
  }

  @Override
  public boolean connect(SocketAddress socketAddress) throws IOException
  {
    return false;
  }

  @Override
  public boolean finishConnect() throws IOException
  {
    return false;
  }

  @Override
  public synchronized int read(ByteBuffer buffer) throws IOException
  {
    int bufferInitialPosition = buffer.position();
    if (applicationInboundBuffer.hasRemaining())
    {
      while (buffer.position() < buffer.limit() && applicationInboundBuffer.hasRemaining())
      {
        buffer.put(applicationInboundBuffer);
      }

      if (applicationInboundBuffer.hasRemaining())
      {
        return buffer.position() - bufferInitialPosition;
      }
      else
      {
        applicationInboundBuffer.clear();
      }

      int remaingBefore = applicationInboundBuffer.limit() - applicationInboundBuffer.position();
      applicationInboundBuffer.put(buffer);
      if (applicationInboundBuffer.hasRemaining())
      {
        int remaingAfter = applicationInboundBuffer.limit() - applicationInboundBuffer.position();
        return remaingBefore - remaingAfter;
      }
      else
      {
        applicationInboundBuffer.clear();
      }
    }

    if (networkInboundBuffer.hasRemaining())
    {
      sslEngine.unwrap(networkInboundBuffer, applicationInboundBuffer);
    }


    networkInboundBuffer.clear();
    int read = socketChannel.read(networkInboundBuffer);


    if (socketChannel.socket().isInputShutdown())
    {
      throw new ClosedChannelException();
    }

    if (!handShakeComplete)
    {
      //   handshake(SelectionKey.OP_READ);
    }

    return 0;
  }

  @Override
  public long read(ByteBuffer[] byteBuffers, int i, int i1) throws IOException
  {
    return 0;
  }

  @Override
  public int write(ByteBuffer byteBuffer) throws IOException
  {
    return 0;
  }

  @Override
  public long write(ByteBuffer[] byteBuffers, int i, int i1) throws IOException
  {
    return 0;
  }

  HandshakeStatus runHandshakeTasks()
  {
    Runnable runnable;
    while ((runnable = sslEngine.getDelegatedTask()) != null)
    {
      executorService.execute(runnable);
    }
    return sslEngine.getHandshakeStatus();
  }

  @Override
  protected void implCloseSelectableChannel() throws IOException
  {

  }

  @Override
  protected void implConfigureBlocking(boolean b) throws IOException
  {

  }
}

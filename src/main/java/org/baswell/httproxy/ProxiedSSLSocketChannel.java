package org.baswell.httproxy;

/*
http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
 */
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
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

public class ProxiedSSLSocketChannel extends SocketChannel implements SSLSocketChannel
{
  static public ProxiedSSLSocketChannel create(InetSocketAddress address, SSLContext sslContext, ExecutorService executorService) throws IOException
  {
    SocketChannel socketChannel = SocketChannel.open(address);
    socketChannel.configureBlocking(false);
    SSLEngine sslEngine = sslContext.createSSLEngine(address.getHostName(), address.getPort());
    sslEngine.setUseClientMode(true);

    return new ProxiedSSLSocketChannel(socketChannel, sslEngine, executorService);
  }



  public final int applicationBufferSize;

  private final SocketChannel socketChannel;

  private final SSLEngine sslEngine;

  private final ExecutorService executorService;

  private final ByteBuffer networkInboundBuffer;

  private final ByteBuffer networkOutboundBuffer;

  private boolean handShakeComplete;

  private boolean networkBufferHasData;

  public ProxiedSSLSocketChannel(SocketChannel socketChannel, SSLEngine sslEngine, ExecutorService executorService) throws IOException
  {
    super(socketChannel.provider());
    this.socketChannel = socketChannel;
    this.sslEngine = sslEngine;
    this.executorService = executorService;

    SSLSession session = sslEngine.getSession();
    applicationBufferSize = session.getApplicationBufferSize();
    int networkBufferSize = session.getPacketBufferSize();
    networkInboundBuffer = ByteBuffer.allocate(networkBufferSize);
    networkOutboundBuffer = ByteBuffer.allocate(networkBufferSize);
  }

  @Override
  public int getApplicationBufferSize()
  {
    return applicationBufferSize;
  }

  @Override
   public SocketChannel getUnderlyingSocketChannel()
  {
    return socketChannel;
  }

  @Override
  public Socket socket()
  {
    return socketChannel.socket();
  }

  @Override
  public boolean isConnected()
  {
    return socketChannel.isConnected();
  }

  @Override
  public boolean isConnectionPending()
  {
    return socketChannel.isConnectionPending();
  }

  @Override
  public boolean connect(SocketAddress socketAddress) throws IOException
  {
    return socketChannel.connect(socketAddress);
  }

  @Override
  public boolean finishConnect() throws IOException
  {
    return socketChannel.finishConnect();
  }

  @Override
  public synchronized int read(ByteBuffer applicationBuffer) throws IOException
  {
    int readFromSocket = socketChannel.read(networkInboundBuffer);

    if (readFromSocket <= 0)
    {
      return readFromSocket;
    }
    else
    {
      networkInboundBuffer.flip();
      try
      {
        SSLEngineResult result = sslEngine.unwrap(networkInboundBuffer, applicationBuffer);

        switch (result.getStatus())
        {
          case OK:
            switch (result.getHandshakeStatus())
            {
              case NEED_TASK:
                Runnable runnable;
                while ((runnable = sslEngine.getDelegatedTask()) != null)
                {
                  executorService.execute(runnable);
                }
                break;
            }

            return result.bytesProduced();

          case CLOSED:
            return -1;

          case BUFFER_OVERFLOW:
            throw new IOException("SSL buffer overflow. Application buffer size must be increased.");

          case BUFFER_UNDERFLOW:
            // Need to read more data from network to perform SSL decrypt
            return 0;
        }
      }
      finally
      {
        networkInboundBuffer.compact();
      }
    }
  }

  @Override
  public long read(ByteBuffer[] byteBuffers, int i, int i1) throws IOException
  {
    return 0;
  }

  @Override
  public int write(ByteBuffer applicationBuffer) throws IOException
  {
    networkOutboundBuffer.compact();
    SSLEngineResult result = sslEngine.wrap(applicationBuffer, networkOutboundBuffer);

    switch (result.getStatus())
    {
      case
    }

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

  void unflip(ByteBuffer buffer)
  {
    buffer.position(buffer.limit());
    buffer.limit(buffer.capacity());

  }
}
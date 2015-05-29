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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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

  private final ByteBuffer applicationInboundBuffer;

  private final ByteBuffer networkOutboundBuffer;

  private final ByteBuffer applicationOutboundBuffer;

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

    applicationInboundBuffer = ByteBuffer.allocate(applicationBufferSize);
    applicationInboundBuffer.flip();

    networkOutboundBuffer = ByteBuffer.allocate(networkBufferSize);
    networkOutboundBuffer.flip();

    applicationOutboundBuffer = ByteBuffer.allocate(applicationBufferSize);
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
  synchronized public int read(ByteBuffer applicationBuffer) throws IOException
  {
    int intialPosition = applicationBuffer.position();

    if (applicationInboundBuffer.hasRemaining())
    {
      applicationBuffer.put(applicationInboundBuffer);
    }

    int readFromChannel = unwrap(true);
    if (applicationInboundBuffer.hasRemaining() && applicationBuffer.hasRemaining())
    {
      applicationBuffer.put(applicationInboundBuffer);
    }

    int totalRead = applicationBuffer.position() - intialPosition;
    if (totalRead == 0 && readFromChannel < 0)
    {
      totalRead = readFromChannel;
    }
    else if (applicationOutboundBuffer.hasRemaining())
    {
      wrap(true);
    }


    System.out.println("read: total read: " + totalRead);
    return totalRead;
  }

  @Override
  synchronized public int write(ByteBuffer applicationBuffer) throws IOException
  {
    // 1. Fill applicationOutboundBuffer

    int initialPosition = applicationBuffer.position();
    applicationOutboundBuffer.put(applicationBuffer);

    // 2. Wrap data and attempt to send to network peer

    int writtenToChannel = wrap(true);
    int totalWritten = applicationBuffer.position() - initialPosition;
    if (totalWritten == 0 && writtenToChannel < 0)
    {
      totalWritten = writtenToChannel;
    }

    System.out.println("write: total written: " + totalWritten);
    return totalWritten;
  }

  /*
   * Buffer pre and post conditions:
   *
   * networkInboundBuffer pre: write, post: write
   * applicationInboundBuffer pre: read, post: read
   *
   */
  int unwrap(boolean wrapIfNeeded) throws IOException
  {
    System.out.println("unwrap:");

    int totalReadFromSocket = 0;

    applicationInboundBuffer.compact();
    try
    {
      // Keep looping until peer has no more data ready or the applicationInboundBuffer is full
      while (true)
      {
        // 1. Pull data from peer into networkInboundBuffer

        int readFromSocket = 0;
        while (networkInboundBuffer.hasRemaining())
        {
          int read = socketChannel.read(networkInboundBuffer);
          System.out.println("unwrap: socket read " + read + "(" + readFromSocket + ", " + totalReadFromSocket + ")");
          if (read <= 0)
          {
            if ((read < 0) && (readFromSocket == 0) && (totalReadFromSocket == 0))
            {
              // No work done and we've reached the end of the channel from peer
              System.out.println("unwrap: exit: end of channel");
              return read;
            }
            break;
          }
          else
          {
            readFromSocket += read;
          }
        }

        networkInboundBuffer.flip();
        if (readFromSocket == 0 && !networkInboundBuffer.hasRemaining())
        {
          networkInboundBuffer.compact();
          return totalReadFromSocket;

          /*
          SSLEngineResult result = sslEngine.unwrap(networkInboundBuffer, applicationInboundBuffer);
          networkInboundBuffer.compact();
          System.out.println("unwrap: exit: no data read");
          return totalReadFromSocket;
          */
        }

        totalReadFromSocket += readFromSocket;

        try
        {
          SSLEngineResult result = sslEngine.unwrap(networkInboundBuffer, applicationInboundBuffer);
          System.out.println("unwrap: result: " + result);

          switch (result.getStatus())
          {
            case OK:
              HandshakeStatus handshakeStatus = result.getHandshakeStatus();
              switch (handshakeStatus)
              {
                case NEED_UNWRAP:
                  break;

                case NEED_WRAP:
                  if (wrap(true) == 0)
                  {
                    System.out.println("unwrap: exit: wrap needed with no data written");
                    return totalReadFromSocket;
                  }
                  break;
                  /*
                  if (wrap(false) == 0)
                  {
                    System.out.println("unwrap: exit: wrap needed with no data written");
                    return totalReadFromSocket;
                  }
                  */

                  //return totalReadFromSocket;
                  /*
                  if (wrapIfNeeded)
                  {
                    if (wrap(false) == 0)
                    {
                      System.out.println("unwrap: exit: wrap needed with no data written");
                      return totalRead;
                    }
                  }
                  else
                  {
                    System.out.println("unwrap: exit: wrap needed not allowed");
                    return totalRead;
                  }
                  break;
                  */

                case NEED_TASK:
                  runHandshakeTasks();
                  System.out.println("unwrap: exit: need tasks");
                  break;

                case NOT_HANDSHAKING:
                default:
                  break;
              }
              break;

            case BUFFER_OVERFLOW:
              // Assume that we've already made progressed and put data in applicationInboundBuffer
              return totalReadFromSocket;

            case CLOSED:
              System.out.println("unwrap: exit: ssl closed");
              return totalReadFromSocket == 0 ? -1 : totalReadFromSocket;

            case BUFFER_UNDERFLOW:
              // Assume that we've already made progressed and put data in applicationInboundBuffer
              return totalReadFromSocket;
          }
        }
        finally
        {
          networkInboundBuffer.compact();
        }
      }
    }
    finally
    {
      applicationInboundBuffer.flip();
    }
  }

  /*
   * Buffer pre and post conditions:
   *
   * networkOutboundBuffer pre: read, post read:
   * applicationOutboundBuffer pre: write, post: write
   *
   */
  int wrap(boolean unwrapIfNecessary) throws IOException
  {
    System.out.println("wrap");
    int totalWritten = 0;

    sslEngine.wrap(applicationOutboundBuffer, networkOutboundBuffer);

    // 1. Any data already wrapped ? Go ahead and send that.
    while (networkOutboundBuffer.hasRemaining())
    {
      int written = socketChannel.write(networkOutboundBuffer);
      totalWritten += written;
      System.out.println("wrap: pre socket write: " + written + " (" + totalWritten + ")");

      if (written <= 0)
      {
        return (totalWritten == 0 && written < 0) ? written : totalWritten;
      }
    }

    // TODO: Should we continue wrapping data here if networkOutboundBuffer isn't full but we weren't able to send any data above to the channel ?

    // 2. Any data in application buffer ? Wrap that and send it to peer.

    applicationOutboundBuffer.flip();
    networkOutboundBuffer.compact();
    try
    {
      WRAP: while (applicationOutboundBuffer.hasRemaining() || networkOutboundBuffer.hasRemaining())
      {
        SSLEngineResult result = sslEngine.wrap(applicationOutboundBuffer, networkOutboundBuffer);
        System.out.println("wrap: result: " + result);
        networkOutboundBuffer.flip();
        try
        {
          // Was any encrypted application data produced ? If so go ahead and try to send to peer.
          int written = 0;
          while (networkOutboundBuffer.hasRemaining())
          {
            int nextWritten = socketChannel.write(networkOutboundBuffer);
            System.out.println("wrap: post socket write: " + nextWritten + " (" + written + ")");

            if (nextWritten == 0)
            {
              break;
            }
            else if (nextWritten < 0)
            {
              totalWritten += written;
              return (totalWritten == 0) ? nextWritten : totalWritten;
            }
            written += nextWritten;
          }

          System.out.println("wrap: post socket write: " + written + " (" + totalWritten + ")");

          totalWritten += written;

          switch (result.getStatus())
          {
            case OK:
              HandshakeStatus handshakeStatus = result.getHandshakeStatus();
              switch (handshakeStatus)
              {
                case NEED_WRAP:
                  // Not enough data in applicationOutboundBuffer.
                  if (written == 0)
                  {
                    System.out.println("wrap: exit: need wrap & no data written");
                    break WRAP;
                  }
                  break;

                case NEED_UNWRAP:
                  if (unwrap(false) == 0)
                  {
                    break WRAP;
                  }
                  break;
                  /*
                  if (unwrap(false) == 0)
                  {
                    // Don't hold selector thread up waiting on data from peer
                    System.out.println("wrap: exit: unwrap and no data read");
                    break WRAP;
                  }
                  */
                  //break UNWRAP;
                  /*
                  if (unwrapIfNecessary)
                  {
                    if (unwrap(false) == 0)
                    {
                      // Don't hold selector thread up waiting on data from peer
                      System.out.println("wrap: exit: unwrap and no data read");
                      break UNWRAP;
                    }
                    else
                    {
                      break;
                    }
                  }
                  else
                  {
                    System.out.println("wrap: exit: unwrap and unwrap not allowed");
                    break UNWRAP;
                  }
                  */

                case NEED_TASK:
                  runHandshakeTasks();
                  System.out.println("wrap: exit: need tasks");
                  break;

                case NOT_HANDSHAKING:
                  if (written <= 0)
                  {
                    System.out.println("wrap: exit: no data written");
                    break WRAP;
                  }
              }
              break;

            case BUFFER_OVERFLOW:
              throw new IOException("Buffer overflow.");

            case CLOSED:
              System.out.println("wrap: exit: closed");
              break WRAP;

            case BUFFER_UNDERFLOW:
              // Need more data in applicationOutboundBuffer
              System.out.println("wrap: exit: buffer underflow");
              break WRAP;
          }
        }
        finally
        {
          networkOutboundBuffer.compact();
        }
      }
    }
    finally
    {
      applicationOutboundBuffer.compact();
      networkOutboundBuffer.flip();
    }

    System.out.println("wrap: return: " + totalWritten);

    return totalWritten;
  }


  @Override
  public long read(ByteBuffer[] byteBuffers, int offset, int length) throws IOException
  {
    long totalRead = 0;
    for (int i = offset; i < length; i++)
    {
      ByteBuffer byteBuffer = byteBuffers[i];
      if (byteBuffer.hasRemaining())
      {
        int read = read(byteBuffer);
        if (read > 0)
        {
          totalRead += read;
          if (byteBuffer.hasRemaining())
          {
            break;
          }
        }
        else
        {
          if ((read < 0) && (totalRead == 0))
          {
            totalRead = -1;
          }
          break;
        }
      }
    }
    return totalRead;
  }

  @Override
  public long write(ByteBuffer[] byteBuffers, int offset, int length) throws IOException
  {
    long totalWritten = 0;
    for (int i = offset; i < length; i++)
    {
      ByteBuffer byteBuffer = byteBuffers[i];
      if (byteBuffer.hasRemaining())
      {
        int written = write(byteBuffer);
        if (written > 0)
        {
          totalWritten += written;
          if (byteBuffer.hasRemaining())
          {
            break;
          }
        }
        else
        {
          if ((written < 0) && (totalWritten == 0))
          {
            totalWritten = -1;
          }
          break;
        }
      }
    }
    return totalWritten;
  }

  @Override
  public Socket socket ()
  {
    return socketChannel.socket();
  }

  @Override
  public boolean isConnected ()
  {
    return socketChannel.isConnected();
  }

  @Override
  public boolean isConnectionPending ()
  {
    return socketChannel.isConnectionPending();
  }

  @Override
  public boolean connect (SocketAddress socketAddress)throws IOException
  {
    return socketChannel.connect(socketAddress);
  }

  @Override
  public boolean finishConnect ()throws IOException
  {
    return socketChannel.finishConnect();
  }

  @Override
  protected void implCloseSelectableChannel ()throws IOException
  {
    if (networkOutboundBuffer.hasRemaining())
    {
      try
      {
        socketChannel.write(networkOutboundBuffer);
      }
      catch (Exception e)
      {
      }
    }

    socketChannel.close();
    sslEngine.closeInbound();
    sslEngine.closeOutbound();
  }

  @Override
  protected void implConfigureBlocking ( boolean b)throws IOException
  {
    socketChannel.configureBlocking(b);
  }

  void runHandshakeTasks ()
  {
    Runnable runnable;
    while ((runnable = sslEngine.getDelegatedTask()) != null)
    {
      runnable.run();
      //executorService.execute(runnable);
    }
  }

}
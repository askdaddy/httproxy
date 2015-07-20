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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * A wrapper around a real {@link SocketChannel} that adds SSL support.
 */
public class SSLSocketChannelOrig extends SocketChannel implements WrappedSocketChannel
{
  public static void main(String[] args) throws Exception
  {
    ByteBuffer buffer = ByteBuffer.allocate(10);

    for (int i = 0; i < 20; i++)
    {
      System.out.println(buffer.position() + " " + buffer.limit() + " " + buffer.remaining() + " " + buffer.hasRemaining());
      buffer.put((byte)i);
    }
  }

  private final SocketChannel socketChannel;

  private final SSLEngine sslEngine;

  private final ExecutorService executorService;

  private final ProxyLogger log;

  private final boolean logDebug;

  private final ByteBuffer networkInboundBuffer;

  private final ByteBuffer networkOutboundBuffer;

  private final ByteBuffer applicationStagingInboundBuffer;

  private final ByteBuffer applicationStagingOutboundBuffer;

  private final int minimumApplicationBufferSize;

  /**
   *
   * @param socketChannel The real SocketChannel.
   * @param sslEngine The SSL engine to use for traffic back and forth on the given SocketChannel.
   * @param executorService Used to execute long running, blocking SSL operations such as certificate validation with a CA (<a href="http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLEngineResult.HandshakeStatus.html#NEED_TASK">NEED_TASK</a>)
   * @param log The logger for debug and error messages. A null logger will result in no log operations.
   * @throws IOException
   */
  public SSLSocketChannelOrig(SocketChannel socketChannel, final SSLEngine sslEngine, ExecutorService executorService, ProxyLogger log)
  {
    super(socketChannel.provider());
    this.socketChannel = socketChannel;
    this.sslEngine = sslEngine;
    this.executorService = executorService;
    this.log = log;

    logDebug = true;// log != null && log.logDebugs();

    SSLSession session = sslEngine.getSession();
    minimumApplicationBufferSize = session.getApplicationBufferSize();
    int networkBufferSize = session.getPacketBufferSize();

    networkInboundBuffer = ByteBuffer.allocate(networkBufferSize);

    networkOutboundBuffer = ByteBuffer.allocate(networkBufferSize);
    networkOutboundBuffer.flip();

    applicationStagingInboundBuffer = ByteBuffer.allocate(minimumApplicationBufferSize);

    applicationStagingOutboundBuffer = ByteBuffer.allocate(minimumApplicationBufferSize);
    applicationStagingOutboundBuffer.flip();
  }

  @Override
   public SocketChannel getWrappedSocketChannel()
  {
    return socketChannel;
  }

  @Override
  synchronized public int read(ByteBuffer applicationBuffer) throws IOException
  {
    System.out.println("on read: " + applicationBuffer.position() + " " + applicationBuffer.limit());
    int intialPosition = applicationBuffer.position();
    int readFromChannel = unwrap(true, applicationBuffer);
    System.out.println("on read: read from channel: " + readFromChannel);

    if (readFromChannel < 0)
    {
      System.out.println("on read: read channel closed");
      return readFromChannel;
    }
    else
    {
      wrap(false, ByteBuffer.allocate(0));
      int totalRead = applicationBuffer.position() - intialPosition;
      if (logDebug) log.info("on read: total read: " + totalRead);
      return totalRead;
    }
  }

  @Override
  synchronized public int write(ByteBuffer applicationBuffer) throws IOException
  {
    System.out.println("on write");
    int intialPosition = applicationBuffer.position();
    int writtenToChannel = wrap(true, applicationBuffer);

    if (writtenToChannel < 0)
    {
      System.out.println("Write channel closed");
      return writtenToChannel;
    }
    else
    {
      unwrap(false, ByteBuffer.allocate(0));
      int totalWritten = applicationBuffer.position() - intialPosition;
      System.out.println("on write: total written: " + totalWritten + " amound available in network outbound: " + applicationBuffer.remaining());
      //if (logDebug) log.info("write: total written: " + totalWritten);
      return totalWritten;
    }
  }

  /*
   * Buffer pre and post conditions:
   *
   * networkInboundBuffer pre: write, post: write
   * applicationInboundBuffer pre: read, post: read
   *
   */
  synchronized int unwrap(boolean wrapIfNecessary, ByteBuffer applicationInboundBuffer) throws IOException
  {
    if (logDebug) log.info("unwrap:");

    int totalReadFromSocket = 0;

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
          if (logDebug) log.info("unwrap: socket read " + read + "(" + readFromSocket + ", " + totalReadFromSocket + ")");
          if (read <= 0)
          {
            if ((read < 0) && (readFromSocket == 0) && (totalReadFromSocket == 0))
            {
              // No work done and we've reached the end of the channel from peer
              if (logDebug) log.info("unwrap: exit: end of channel");
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
          //networkInboundBuffer.compact();
          //return totalReadFromSocket;
        }

        totalReadFromSocket += readFromSocket;

        try
        {
          System.out.println("unwrap: network inbound remaining: " + networkInboundBuffer.remaining() + " application inbound: " + applicationInboundBuffer.position() + " " + applicationInboundBuffer.limit());
          SSLEngineResult result = sslEngine.unwrap(networkInboundBuffer, applicationInboundBuffer);
          System.out.println("unwrap: network inbound remaining: " + networkInboundBuffer.remaining() + " application inbound: " + applicationInboundBuffer.position());
          if (logDebug) log.info("unwrap: result: " + result);

          switch (result.getStatus())
          {
            case OK:
              HandshakeStatus handshakeStatus = result.getHandshakeStatus();
              switch (handshakeStatus)
              {
                case NEED_UNWRAP:
                  break;

                case NEED_WRAP:
                  if (wrap(true, applicationStagingOutboundBuffer) == 0)
                  {
                    if (logDebug) log.info("unwrap: exit: wrap needed with no data written");
                    return totalReadFromSocket;
                  }
                  break;

                case NEED_TASK:
                  runHandshakeTasks();
                  if (logDebug) log.info("unwrap: exit: need tasks");
                  return totalReadFromSocket;

                case NOT_HANDSHAKING:
                default:
                  break;
              }
              break;

            case BUFFER_OVERFLOW:
              // Assume that we've already made progressed and put data in applicationInboundBuffer
              return totalReadFromSocket;

            case CLOSED:
              if (logDebug) log.info("unwrap: exit: ssl closed");
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
      System.out.println("unwrap: application staging: " + applicationInboundBuffer.position());
//      applicationInboundBuffer.flip();
    }
  }

  /*
   * Buffer pre and post conditions:
   *
   * networkOutboundBuffer pre: read, post read:
   * applicationOutboundBuffer pre: write, post: write
   *
   */
  synchronized int wrap(boolean unwrapIfNecessary, ByteBuffer applicationOutboundBuffer) throws IOException
  {
    if (logDebug) log.info("wrap");
    int totalWritten = 0;

   // sslEngine.wrap(applicationOutboundBuffer, networkOutboundBuffer);

    // 1. Any data already wrapped ? Go ahead and send that.
    while (networkOutboundBuffer.hasRemaining())
    {
      int written = socketChannel.write(networkOutboundBuffer);
      totalWritten += written;
      if (logDebug) log.info("wrap: pre socket write: " + written + " (" + totalWritten + ")");

      if (written <= 0)
      {
        return (totalWritten == 0 && written < 0) ? written : totalWritten;
      }
    }

    if (networkOutboundBuffer.hasRemaining())
    {
      return totalWritten;
    }

    // 2. Any data in application buffer ? Wrap that and send it to peer.

    networkOutboundBuffer.compact();
    try
    {
      WRAP: while (applicationOutboundBuffer.hasRemaining() || networkOutboundBuffer.hasRemaining())
      {
        System.out.println("wrap: application outbound: " + applicationOutboundBuffer.remaining());
        SSLEngineResult result = sslEngine.wrap(applicationOutboundBuffer, networkOutboundBuffer);
        System.out.println("wrap: application outbound: " + applicationOutboundBuffer.remaining());
        if (logDebug) log.info("wrap: result: " + result);
        networkOutboundBuffer.flip();
        try
        {
          // Was any encrypted application data produced ? If so go ahead and try to send to peer.
          int written = 0;
          while (networkOutboundBuffer.hasRemaining())
          {
            int nextWritten = socketChannel.write(networkOutboundBuffer);

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

          if (logDebug) log.info("wrap: socket write: " + written + " (" + totalWritten + ")");

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
                    if (logDebug) log.info("wrap: exit: need wrap & no data written");
                    break WRAP;
                  }
                  break;

                case NEED_UNWRAP:
                  applicationStagingInboundBuffer.clear();
                  if (unwrap(false, applicationStagingInboundBuffer) == 0)
                  {
                    break WRAP;
                  }
                  break;
                  /*
                  if (unwrap(false) == 0)
                  {
                    // Don't hold selector thread up waiting on data from peer
                    if (logDebug) log.info("wrap: exit: unwrap and no data read");
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
                      if (logDebug) log.info("wrap: exit: unwrap and no data read");
                      break UNWRAP;
                    }
                    else
                    {
                      break;
                    }
                  }
                  else
                  {
                    if (logDebug) log.info("wrap: exit: unwrap and unwrap not allowed");
                    break UNWRAP;
                  }
                  */

                case NEED_TASK:
                  runHandshakeTasks();
                  if (logDebug) log.info("wrap: exit: need tasks");
                  break;

                case NOT_HANDSHAKING:
                  if (written <= 0)
                  {
                    if (logDebug) log.info("wrap: exit: no data written");
                    break WRAP;
                  }
              }
              break;

            case BUFFER_OVERFLOW:
              throw new IOException("Buffer overflow.");
              //System.out.println(applicationOutboundBuffer.limit() + " " + applicationOutboundBuffer.remaining() + " / " + networkOutboundBuffer.limit() + " " + networkOutboundBuffer.remaining());
              //break WRAP;

            case CLOSED:
              if (logDebug) log.info("wrap: exit: closed");
              break WRAP;

            case BUFFER_UNDERFLOW:
              // Need more data in applicationOutboundBuffer
              if (logDebug) log.info("wrap: exit: buffer underflow");
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
      networkOutboundBuffer.flip();
    }

    if (logDebug) log.info("wrap: return: " + totalWritten);

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
  public SocketAddress getLocalAddress() throws IOException
  {
    return null;
  }

  @Override
  public SocketChannel bind(SocketAddress local) throws IOException
  {
    return null;
  }

  @Override
  public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException
  {
    return null;
  }

  @Override
  public <T> T getOption(SocketOption<T> name) throws IOException
  {
    return null;
  }

  @Override
  public Set<SocketOption<?>> supportedOptions()
  {
    return null;
  }

  @Override
  public SocketChannel shutdownInput() throws IOException
  {
    return null;
  }

  @Override
  public SocketChannel shutdownOutput() throws IOException
  {
    return null;
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
  public SocketAddress getRemoteAddress() throws IOException
  {
    return null;
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
    while (true)
    {
      final Runnable runnable = sslEngine.getDelegatedTask();
      if (runnable == null)
      {
        break;
      }
      else
      {
        executorService.execute(runnable);
      }
    }
  }
}
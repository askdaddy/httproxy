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
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

/**
 * A wrapper around a real {@link SocketChannel} that adds SSL support.
 */
class SSLSocketChannel extends SocketChannel implements WrappedSocketChannel
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

  private final SSLEngineBuffer sslEngineBuffer;

  private final ProxyLogger log;
  
  private final boolean logDebug;

  /**
   *
   * @param socketChannel The real SocketChannel.
   * @param sslEngine The SSL engine to use for traffic back and forth on the given SocketChannel.
   * @param executorService Used to execute long running, blocking SSL operations such as certificate validation with a CA (<a href="http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLEngineResult.HandshakeStatus.html#NEED_TASK">NEED_TASK</a>)
   * @param log The logger for debug and error messages. A null logger will result in no log operations.
   * @throws IOException
   */
  public SSLSocketChannel(SocketChannel socketChannel, final SSLEngine sslEngine, ExecutorService executorService, ProxyLogger log)
  {
    super(socketChannel.provider());

    this.socketChannel = socketChannel;
    this.log = new WrappedLogger(log);

    logDebug = log.logDebugs();

    sslEngineBuffer = new SSLEngineBuffer(socketChannel, sslEngine, executorService, log);
  }

  @Override
   public SocketChannel getWrappedSocketChannel()
  {
    return socketChannel;
  }

  @Override
  synchronized public int read(ByteBuffer applicationBuffer) throws IOException
  {
    if (logDebug) log.debug("read: " + applicationBuffer.position() + " " + applicationBuffer.limit());
    int intialPosition = applicationBuffer.position();

    int readFromChannel = sslEngineBuffer.unwrap(applicationBuffer);
    if (logDebug) log.debug("read: from channel: " + readFromChannel);

    if (readFromChannel < 0)
    {
      if (logDebug) log.debug("read: channel closed.");
      return readFromChannel;
    }
    else
    {
      int totalRead = applicationBuffer.position() - intialPosition;
      if (logDebug) log.info("read: total read: " + totalRead);
      return totalRead;
    }
  }

  @Override
  synchronized public int write(ByteBuffer applicationBuffer) throws IOException
  {
    if (logDebug) log.debug("write:");

    int intialPosition = applicationBuffer.position();
    int writtenToChannel = sslEngineBuffer.wrap(applicationBuffer);

    if (writtenToChannel < 0)
    {
      if (logDebug) log.debug("write: channel closed");
      return writtenToChannel;
    }
    else
    {
      int totalWritten = applicationBuffer.position() - intialPosition;
      if (logDebug) log.debug("write: total written: " + totalWritten + " amount available in network outbound: " + applicationBuffer.remaining());
      return totalWritten;
    }
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
    try
    {
      sslEngineBuffer.flushNetworkOutbound();
    }
    catch (Exception e)
    {}

    socketChannel.close();
    sslEngineBuffer.close();
  }

  @Override
  protected void implConfigureBlocking ( boolean b)throws IOException
  {
    socketChannel.configureBlocking(b);
  }
}
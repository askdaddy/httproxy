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
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class SSLEngineBuffer
{
  private final SocketChannel socketChannel;

  private final SSLEngine sslEngine;

  private final ExecutorService executorService;

  private final ByteBuffer networkInboundBuffer;

  private final ByteBuffer networkOutboundBuffer;

  private final int applicationBufferSize;

  private final ByteBuffer unwrapBuffer;

  private final ByteBuffer wrapBuffer;

  private final ProxyLogger log;

  private final boolean logDebug;

  public SSLEngineBuffer(SocketChannel socketChannel, SSLEngine sslEngine, ExecutorService executorService, ProxyLogger log)
  {
    this.socketChannel = socketChannel;
    this.sslEngine = sslEngine;
    this.executorService = executorService;
    this.log = log;

    logDebug = log.logDebugs();

    SSLSession session = sslEngine.getSession();
    int networkBufferSize = session.getPacketBufferSize();

    networkInboundBuffer = ByteBuffer.allocate(networkBufferSize);

    networkOutboundBuffer = ByteBuffer.allocate(networkBufferSize);
    networkOutboundBuffer.flip();


    /*
     * Due to synchronous nature of HTTP, A response for a request should (almost never) be returned before a request
     * is fully sent. With this assertion in mind we assume that if a read operation is taking place and the SSLEngine
     * needs to perform a wrap operation, all data wrapped will be SSL related. Likewise when performing a write if a unwrap
     * needs to take place we assume only SSL data will be retrieved from the network channel. Therefore the following buffers
     * are used to satisfy SSLEngine.wrap and SSLEngine.unwrap but are otherwise ignored.
     *
     * There is an edge case that will break this. In Section 8.2.2 of RFC 2616 the following is stated:
     *
     * An HTTP/1.1 (or later) client sending a message-body SHOULD monitor the network connection for an error status
     * while it is transmitting the request. If the client sees an error status, it SHOULD immediately cease
     * transmitting the body.
     *
     * So if that were to ever take place and data was actually ever placed in the unwrapBuffer that would break this
     * implementation because that data would never get back to the application.
     */

    applicationBufferSize = session.getApplicationBufferSize();
    unwrapBuffer = ByteBuffer.allocate(applicationBufferSize);
    wrapBuffer = ByteBuffer.allocate(applicationBufferSize);
    wrapBuffer.flip();
  }

  int unwrap(ByteBuffer applicationInputBuffer) throws IOException
  {
    if (applicationInputBuffer.capacity() < applicationBufferSize)
    {
      throw new IOException("Buffer size must be at least: " + applicationBufferSize + " for non-blocking IO over SSL.");
    }

    if (unwrapBuffer.position() != 0)
    {
      unwrapBuffer.flip();
      while (unwrapBuffer.hasRemaining() && applicationInputBuffer.hasRemaining())
      {
        applicationInputBuffer.put(unwrapBuffer.get());
      }
      unwrapBuffer.compact();
    }

    int totalUnwrapped = 0;
    int unwrapped, wrapped;

    do
    {
      totalUnwrapped += unwrapped = doUnwrap(applicationInputBuffer);
      wrapped = doWrap(wrapBuffer);
    }
    while (unwrapped > 0 || wrapped > 0 && (networkOutboundBuffer.hasRemaining() && networkInboundBuffer.hasRemaining()));

    return totalUnwrapped;
  }

  int wrap(ByteBuffer applicationOutboundBuffer) throws IOException
  {
    int wrapped = doWrap(applicationOutboundBuffer);
    doUnwrap(unwrapBuffer);
    return wrapped;
  }

  int flushNetworkOutbound() throws IOException
  {
    return send(socketChannel, networkOutboundBuffer);
  }

  int send(SocketChannel channel, ByteBuffer buffer) throws IOException
  {
    int totalWritten = 0;
    while (buffer.hasRemaining())
    {
      int written = channel.write(buffer);

      if (written == 0)
      {
        break;
      }
      else if (written < 0)
      {
        return (totalWritten == 0) ? written : totalWritten;
      }
      totalWritten += written;
    }
    if (logDebug) log.debug("sent: " + totalWritten + " out to socket");
    return totalWritten;
  }

  void close()
  {
    try
    {
      sslEngine.closeInbound();
    }
    catch (Exception e)
    {}

    try
    {
      sslEngine.closeOutbound();
    }
    catch (Exception e)
    {}
  }

  private int doUnwrap(ByteBuffer applicationInputBuffer) throws IOException
  {
    if (logDebug) log.info("unwrap:");

    int totalReadFromChannel = 0;

    // Keep looping until peer has no more data ready or the applicationInboundBuffer is full
    UNWRAP: do
    {
      // 1. Pull data from peer into networkInboundBuffer

      int readFromChannel = 0;
      while (networkInboundBuffer.hasRemaining())
      {
        int read = socketChannel.read(networkInboundBuffer);
        if (logDebug) log.info("unwrap: socket read " + read + "(" + readFromChannel + ", " + totalReadFromChannel + ")");
        if (read <= 0)
        {
          if ((read < 0) && (readFromChannel == 0) && (totalReadFromChannel == 0))
          {
            // No work done and we've reached the end of the channel from peer
            if (logDebug) log.info("unwrap: exit: end of channel");
            return read;
          }
          break;
        }
        else
        {
          readFromChannel += read;
        }
      }


      networkInboundBuffer.flip();
      if (!networkInboundBuffer.hasRemaining())
      {
        networkInboundBuffer.compact();
        //wrap(applicationOutputBuffer, applicationInputBuffer, false);
        return totalReadFromChannel;
      }

      totalReadFromChannel += readFromChannel;

      try
      {
        SSLEngineResult result = sslEngine.unwrap(networkInboundBuffer, applicationInputBuffer);
        if (logDebug) log.info("unwrap: result: " + result);

        switch (result.getStatus())
        {
          case OK:
            SSLEngineResult.HandshakeStatus handshakeStatus = result.getHandshakeStatus();
            switch (handshakeStatus)
            {
              case NEED_UNWRAP:
                break;

              case NEED_WRAP:
                break UNWRAP;

              case NEED_TASK:
                runHandshakeTasks();
                break;

              case NOT_HANDSHAKING:
              default:
                break;
            }
            break;

          case BUFFER_OVERFLOW:
            if (logDebug) log.debug("unwrap: buffer overflow");
            break UNWRAP;

          case CLOSED:
            if (logDebug) log.info("unwrap: exit: ssl closed");
            return totalReadFromChannel == 0 ? -1 : totalReadFromChannel;

          case BUFFER_UNDERFLOW:
            if (logDebug) log.debug("unwrap: buffer underflow");
            break;
        }
      }
      finally
      {
        networkInboundBuffer.compact();
      }
    }
    while (applicationInputBuffer.hasRemaining());

    return totalReadFromChannel;
  }

  private int doWrap(ByteBuffer applicationOutboundBuffer) throws IOException
  {
    if (logDebug) log.info("wrap");
    int totalWritten = 0;

    // 1. Send any data already wrapped out channel

    if (networkOutboundBuffer.hasRemaining())
    {
      totalWritten = send(socketChannel, networkOutboundBuffer);
      if (totalWritten < 0)
      {
        return totalWritten;
      }
    }

    // 2. Any data in application buffer ? Wrap that and send it to peer.

    WRAP: while (true)
    {
      networkOutboundBuffer.compact();
      SSLEngineResult result = sslEngine.wrap(applicationOutboundBuffer, networkOutboundBuffer);
      if (logDebug) log.info("wrap: result: " + result);

      networkOutboundBuffer.flip();
      if (networkOutboundBuffer.hasRemaining())
      {
        int written = send(socketChannel, networkOutboundBuffer);
        if (written < 0)
        {
          return totalWritten == 0 ? written : totalWritten;
        }
        else
        {
          totalWritten += written;
        }
      }

      switch (result.getStatus())
      {
        case OK:
          switch (result.getHandshakeStatus())
          {
            case NEED_WRAP:
              break;

            case NEED_UNWRAP:
              break WRAP;

            case NEED_TASK:
              runHandshakeTasks();
              if (logDebug) log.info("wrap: exit: need tasks");
              break;

            case NOT_HANDSHAKING:
              if (applicationOutboundBuffer.hasRemaining())
              {
                break;
              }
              else
              {
                break WRAP;
              }
          }
          break;

        case BUFFER_OVERFLOW:
          if (logDebug) log.info("wrap: exit: buffer overflow");
          break WRAP;

        case CLOSED:
          if (logDebug) log.info("wrap: exit: closed");
          break WRAP;

        case BUFFER_UNDERFLOW:
          if (logDebug) log.info("wrap: exit: buffer underflow");
          break WRAP;
      }
    }

    if (logDebug) log.info("wrap: return: " + totalWritten);
    return totalWritten;
  }

  private void runHandshakeTasks ()
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

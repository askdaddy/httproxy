package org.baswell.httproxy;

import com.sun.org.apache.xpath.internal.functions.FuncFalse;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutorService;

public class SSLEngineBuffer
{
  private final SSLEngine sslEngine;

  private final ExecutorService executorService;

  private final ByteBuffer networkInboundBuffer;

  private final ByteBuffer networkOutboundBuffer;

  private final ByteBuffer emptyBuffer;

  private final ProxyLogger log;

  private final boolean logDebug;

  public SSLEngineBuffer(SSLEngine sslEngine, ExecutorService executorService, ProxyLogger log)
  {
    this.sslEngine = sslEngine;
    this.executorService = executorService;
    this.log = log;

    logDebug = log.logDebugs();

    SSLSession session = sslEngine.getSession();
    int networkBufferSize = session.getPacketBufferSize();

    networkInboundBuffer = ByteBuffer.allocate(networkBufferSize);

    networkOutboundBuffer = ByteBuffer.allocate(networkBufferSize);
    networkOutboundBuffer.flip();

    emptyBuffer = ByteBuffer.allocate(0);
  }

  synchronized int unwrap(SocketChannel socketChannel, ByteBuffer inputBuffer, boolean wrapIfNecessary) throws IOException
  {
    if (logDebug) log.info("unwrap:");

    int totalReadFromChannel = 0;

    try
    {
      // Keep looping until peer has no more data ready or the applicationInboundBuffer is full
      WRAP: while (true)
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
        totalReadFromChannel += readFromChannel;

        try
        {
          SSLEngineResult result = sslEngine.unwrap(networkInboundBuffer, inputBuffer);
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
                  if (wrapIfNecessary)
                  {
                    wrap(socketChannel, emptyBuffer, false);
                  }
                  else
                  {
                    break WRAP;
                  }
                  break;

                case NEED_TASK:
                  runHandshakeTasks();
                  break;

                case NOT_HANDSHAKING:
                default:
                  break;
              }
              break;

            case BUFFER_OVERFLOW:
              break WRAP;

            case CLOSED:
              if (logDebug) log.info("unwrap: exit: ssl closed");
              return totalReadFromChannel == 0 ? -1 : totalReadFromChannel;

            case BUFFER_UNDERFLOW:
              break WRAP;
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
      System.out.println("unwrap: application staging: " + inputBuffer.position());
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
  synchronized int wrap(SocketChannel socketChannel, ByteBuffer applicationOutboundBuffer) throws IOException
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
              SSLEngineResult.HandshakeStatus handshakeStatus = result.getHandshakeStatus();
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

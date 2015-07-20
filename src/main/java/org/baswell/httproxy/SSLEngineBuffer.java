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

  private final ByteBuffer emptyInboundBuffer;

  private final ByteBuffer emptyOutboundBuffer;

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

    emptyInboundBuffer = ByteBuffer.allocate(session.getApplicationBufferSize());

    emptyOutboundBuffer = ByteBuffer.allocate(networkBufferSize);
    emptyOutboundBuffer.flip();
  }

  int unwrap(ByteBuffer applicationInputBuffer) throws IOException
  {
    int unwrapped = unwrap(applicationInputBuffer, emptyOutboundBuffer, true);
    wrap(emptyOutboundBuffer, applicationInputBuffer, false);
    return unwrapped;
  }

  synchronized private int unwrap(ByteBuffer applicationInputBuffer, ByteBuffer applicationOutputBuffer, boolean wrapIfNecessary) throws IOException
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
                if (wrapIfNecessary)
                {
                  //wrap(applicationOutputBuffer, applicationInputBuffer, false);
                }
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

  int wrap(ByteBuffer applicationOutboundBuffer) throws IOException
  {
    int wrapped = wrap(applicationOutboundBuffer, emptyInboundBuffer, true);
    unwrap(emptyInboundBuffer, applicationOutboundBuffer, false);
    return wrapped;
  }

  synchronized private int wrap(ByteBuffer applicationOutboundBuffer, ByteBuffer applicationInboundBuffer, boolean unwrapIfNecessary) throws IOException
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

    WRAP: do
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
          SSLEngineResult.HandshakeStatus handshakeStatus = result.getHandshakeStatus();
          switch (handshakeStatus)
          {
            case NEED_WRAP:
              System.out.println("HERE");
              break;

            case NEED_UNWRAP:
              if (unwrapIfNecessary)
              {
                //unwrap(applicationInboundBuffer, applicationOutboundBuffer, false);
              }
              break WRAP;

            case NEED_TASK:
              runHandshakeTasks();
              if (logDebug) log.info("wrap: exit: need tasks");
              break;

            case NOT_HANDSHAKING:
              break WRAP;
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
    while (true);

    if (logDebug) log.info("wrap: return: " + totalWritten);
    return totalWritten;
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
        //executorService.execute(runnable);
        runnable.run();
      }
    }
    System.out.println("******* -> " + sslEngine.getHandshakeStatus());
  }
}

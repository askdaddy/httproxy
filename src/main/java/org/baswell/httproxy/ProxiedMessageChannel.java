package org.baswell.httproxy;

import gnu.trove.list.array.TByteArrayList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

abstract class ProxiedMessageChannel extends ProxiedMessage
{
  protected final ProxiedExchangeChannel proxiedChannel;

  protected final SocketChannel readChannel;

  protected SocketChannel writeChannel;

  private final int maxWriteAttempts;

  ProxiedMessageChannel(boolean request, ProxiedExchangeChannel proxiedChannel, SocketChannel readChannel, NIOProxyDirector proxyDirector)
  {
    this(request, proxiedChannel, readChannel, null, proxyDirector);
    writeBuffer = new TByteArrayList();
  }

  ProxiedMessageChannel(boolean request, ProxiedExchangeChannel proxiedChannel, SocketChannel readChannel, SocketChannel writeChannel, NIOProxyDirector proxyDirector)
  {
    super(request, proxyDirector);

    this.proxiedChannel = proxiedChannel;
    this.readChannel = readChannel;
    this.writeChannel = writeChannel;
    this.maxWriteAttempts = proxyDirector.getMaxWriteAttempts();
  }

  boolean readAndWriteAvailabe() throws ProxiedIOException, HttpProtocolException
  {
    try
    {
      while (true)
      {
        int read;
        if (readBuffer.hasRemaining())
        {
          /*
           * If the previous readAndWriteAvailabe wasn't fully written don't ready anymore until it's fully written out.
           */
          read = readBuffer.remaining();
        }
        else
        {
          readBuffer.clear();
          read = readChannel.read(readBuffer);
          readBuffer.flip();
        }

        if (read == -1)
        {
          throw new ProxiedIOException(request, new IOException("Connection closed."));
        }
        else if (read == 0)
        {
          break;
        }
        else
        {
          if (!readAndWriteBuffer())
          {
            return false;
          }
        }
      }

      return true;
    }
    catch (IOException e)
    {
      throw new ProxiedIOException(request, e);
    }
  }

  @Override
  protected boolean write() throws ProxiedIOException
  {
    try
    {
      if (writeChannel != null)
      {
        int remainingWriteAttempts = maxWriteAttempts;
        /*
         * Some types of channels, depending upon their readState, may write only some of the bytes or possibly none at all.
         * A socket channel in non-blocking mode, for example, cannot write any more bytes than are free in the socket's output readAndWriteBuffer.
         */

        if (writeBuffer != null)
        {
          while ((remainingWriteAttempts-- > 0) && !writeBuffer.isEmpty())
          {
            int written = writeChannel.write(ByteBuffer.wrap(writeBuffer.toArray()));
            if (written < writeBuffer.size())
            {
              writeBuffer = writeBuffer.subList(written, writeBuffer.size());
            }
            else
            {
              writeBuffer.clear();
            }
          }

          if (!writeBuffer.isEmpty())
          {
            return false;
          }
          else
          {
            writeBuffer = null;
            remainingWriteAttempts = maxWriteAttempts;
          }
        }


        if (readBuffer != null)
        {
          readBuffer.reset();
          while ((remainingWriteAttempts-- > 0) && readBuffer.hasRemaining())
          {
            writeChannel.write(readBuffer);
          }
          readBuffer.mark();

          return !readBuffer.hasRemaining();
        }
        else
        {
          return true;
        }
      }
      else if (readBuffer != null)
      {
        readBuffer.reset();
        while (readBuffer.hasRemaining())
        {
          writeBuffer.add(readBuffer.get());
        }
        readBuffer.mark();
        return true;
      }
      else
      {
        return true;
      }
    }
    catch (IOException e)
    {
      throw new ProxiedIOException(request, e);
    }
  }
}

package org.baswell.httproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class HttpMessageStreamMethods
{
  static void doReadAndWriteMessage(HttpMessagePipe messagePipe, InputStream inputStream, byte[] readBytes, int sleepSecondsOnReadWait) throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    try
    {
      while (true)
      {
        int read = inputStream.read(readBytes);

        if (read == -1)
        {
          throw new ProxiedIOException(messagePipe.currentMessage, new IOException("Connection closed."));
        }
        else if (read == 0)
        {
          try
          {
            Thread.sleep(sleepSecondsOnReadWait * 1000);
          }
          catch (InterruptedException e)
          {}
        }
        else
        {
          // TODO How expensive is this ? Does the stream need to work directly on the byte array ?
          messagePipe.readBuffer = ByteBuffer.wrap(readBytes);
          messagePipe.readBuffer.limit(read);

          messagePipe.readAndWriteBuffer();
        }

        if (messagePipe.readState == HttpMessagePipe.ReadState.DONE)
        {
          break;
        }
      }
    }
    catch (IOException e)
    {
      throw new ProxiedIOException(messagePipe.currentMessage, e);
    }
  }

  static boolean doWrite(HttpMessagePipe messagePipe, OutputStream outputStream, byte[] readBytes) throws ProxiedIOException
  {
    try
    {
      if ((messagePipe.writeBuffer != null) && (outputStream != null))
      {
        if (!messagePipe.writeBuffer.isEmpty())
        {
          outputStream.write(messagePipe.writeBuffer.toArray());
        }
        messagePipe.writeBuffer = null;
      }

      messagePipe.readBuffer.reset();
      if (outputStream != null)
      {
        if (readBytes != null)
        {
          outputStream.write(messagePipe.readBuffer.array(), messagePipe.readBuffer.position(), messagePipe.readBuffer.limit() - messagePipe.readBuffer.position());
          outputStream.flush();
        }
      }
      else if (readBytes != null)
      {
        messagePipe.writeBuffer.add(messagePipe.readBuffer.array(), messagePipe.readBuffer.position(), messagePipe.readBuffer.position());
      }
      messagePipe.readBuffer.mark();

      return true;
    }
    catch (IOException e)
    {
      throw new ProxiedIOException(messagePipe.currentMessage, e);
    }
  }
}

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

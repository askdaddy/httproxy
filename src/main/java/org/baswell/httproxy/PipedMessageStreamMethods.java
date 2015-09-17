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

class PipedMessageStreamMethods
{
  static void doReadAndWriteMessage(PipedMessage messagePipe, InputStream inputStream, byte[] readBytes, int sleepSecondsOnReadWait) throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    try
    {
      while (!messagePipe.isReadComplete()) // Stream messages write the full content (blocking) so when read is complete so is write
      {
        int read = inputStream.read(readBytes);

        if (read == -1)
        {
          throw new ProxiedIOException(messagePipe.currentMessage, true, new IOException("Connection closed."));
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
          messagePipe.readBuffer = ByteBuffer.wrap(readBytes);
          messagePipe.readBuffer.limit(read);

          messagePipe.readAndWriteBuffer();
        }
      }
    }
    catch (IOException e)
    {
      throw new ProxiedIOException(messagePipe.currentMessage, true,  e);
    }
  }

  static boolean doWrite(PipedMessage messagePipe, OutputStream outputStream, byte[] readBytes) throws ProxiedIOException
  {
    try
    {
      if (messagePipe.writeBuffer != null && !messagePipe.writeBuffer.isEmpty()  && outputStream != null)
      {
        outputStream.write(messagePipe.writeBuffer.toArray());
        messagePipe.writeBuffer.clear();
      }

      messagePipe.readBuffer.reset();
      int position = messagePipe.readBuffer.position();
      int read = messagePipe.readBuffer.limit() - position;
      if (read > 0)
      {
        if (outputStream != null)
        {
          outputStream.write(messagePipe.readBuffer.array(), position, read);
          outputStream.flush();
        }
        else
        {
          messagePipe.writeBuffer.add(messagePipe.readBuffer.array(), position, read);
        }
      }
      messagePipe.readBuffer.mark();

      return true;
    }
    catch (IOException e)
    {
      throw new ProxiedIOException(messagePipe.currentMessage, false,  e);
    }
  }
}

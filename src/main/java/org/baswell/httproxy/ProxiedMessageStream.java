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

import gnu.trove.list.array.TByteArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

abstract class ProxiedMessageStream extends ProxiedMessage
{
  protected final ProxiedExchangeStream proxiedExchangeStream;

  protected final InputStream inputStream;

  protected OutputStream outputStream;

  protected byte[] readBytes;

  private final int sleepSecondsOnReadWait;

  ProxiedMessageStream(boolean request, ProxiedExchangeStream proxiedExchangeStream, InputStream inputStream, IOProxyDirector proxyDirector)
  {
    this(request, proxiedExchangeStream, inputStream, null, proxyDirector);
    writeBuffer = new TByteArrayList();
  }

  ProxiedMessageStream(boolean request, ProxiedExchangeStream proxiedExchangeStream, InputStream inputStream, OutputStream outputStream, IOProxyDirector proxyDirector)
  {
    super(request, proxyDirector);

    this.proxiedExchangeStream = proxiedExchangeStream;
    this.inputStream = inputStream;
    this.outputStream = outputStream;

    readBytes = new byte[bufferSize];
    sleepSecondsOnReadWait = proxyDirector.getSleepSecondsOnReadWait();
  }

  void readAndWriteMessage() throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    try
    {
      while (true)
      {
        int read = inputStream.read(readBytes);

        if (read == -1)
        {
          throw new ProxiedIOException(request, new IOException("Connection closed."));
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
          readBuffer = ByteBuffer.wrap(readBytes);
          readBuffer.limit(read);

          readAndWriteBuffer();
        }

        if (readState == ReadState.DONE)
        {
          break;
        }
      }
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
      if ((writeBuffer != null) && (outputStream != null))
      {
        if (!writeBuffer.isEmpty())
        {
          outputStream.write(writeBuffer.toArray());
        }
        writeBuffer = null;
      }

      readBuffer.reset();
      if (outputStream != null)
      {
        if (readBytes != null)
        {
          outputStream.write(readBuffer.array(), readBuffer.position(), readBuffer.limit() - readBuffer.position());
          outputStream.flush();
        }
      }
      else if (readBytes != null)
      {
        writeBuffer.add(readBuffer.array(), readBuffer.position(), readBuffer.position());
      }
      readBuffer.mark();

      return true;
    }
    catch (IOException e)
    {
      throw new ProxiedIOException(request, e);
    }
  }
}

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

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import static org.baswell.httproxy.Constants.*;

abstract class PipedMessage
{
  abstract void readStatusLine() throws IOException;

  abstract void onHeadersProcessed() throws IOException, EndProxiedRequestException;

  abstract boolean write() throws ProxiedIOException;

  abstract void onMessageDone() throws IOException;

  final ProxyDirector proxyDirector;

  ByteBuffer readBuffer;

  final int bufferSize;

  ReadState readState = ReadState.READING_STATUS;

  HttpMessage currentMessage;

  final TByteArrayList currentLine = new TByteArrayList(AVERAGE_HEADER_LENGTH);

  TByteList writeBuffer = new TByteArrayList();

  Long contentLength;

  long contentRead;

  int chunkedNextBytesTotal;

  int chunkedNextBytesRead;

  ReadChunkedInputState processChunkedState;


  PipedMessage(ProxyDirector proxyDirector)
  {
    this.proxyDirector = proxyDirector;

    this.bufferSize = proxyDirector.getBufferSize();

    readBuffer = ByteBuffer.allocate(bufferSize);
    readBuffer.compact();
  }

  boolean readAndWriteBuffer() throws ProxiedIOException, IOException, HttpProtocolException, EndProxiedRequestException
  {
    readBuffer.mark();

    if (readState == ReadState.DONE)
    {
      reset();
    }

    READ_STATE_LOOP: while (readBuffer.hasRemaining())
    {
      switch (readState)
      {
        case READING_STATUS:
          readStatusLine();
          break;

        case READING_HEADER:
          readHeaderLine();
          break;

        case READING_FIXED_LENGTH_CONTENT:
          readFixedWidthContent();
          break;

        case READING_CHUNKED_CONTENT:
          processChunkedData();
          break;

        case DONE:
          break READ_STATE_LOOP;
      }
    }

    if (readState == ReadState.DONE)
    {
      currentMessage.endedAt = new Date();
      onMessageDone();
    }

    return write();
  }

  boolean isReadComplete()
  {
    return readState == ReadState.DONE;
  }

  boolean isMessageComplete()
  {
    return isReadComplete() && (writeBuffer == null || writeBuffer.isEmpty());
  }

  void readHeaderLine() throws HttpProtocolException, IOException, EndProxiedRequestException
  {
    byte[] headerLineBytes;
    while ((headerLineBytes = readNextLine(true)) != null)
    {
      readBuffer.mark();
      if (lineHasContent(headerLineBytes))
      {
        HttpHeader header = currentMessage.addHeader(new String(headerLineBytes).trim());
        if (header != null)
        {
          /*
           * The latest HTTP specification defines only one transfer-encoding, chunked encoding
           *
           * If a message is received with both Content-Length and Transfer-Encoding the Content-Length header
           * must be ignored because the transfer encoding will change the way entity bodies are represented and
           * transferred (and number of bytes transmitted).
           */
          if (header.name.equalsIgnoreCase("Transfer-Encoding"))
          {
            if (header.value.equalsIgnoreCase("chunked"))
            {
              processChunkedState = ReadChunkedInputState.READ_BYTE_COUNT;
            }
            else
            {
              throw new HttpProtocolException(currentMessage, "Don't know how to transfer encoding: " + header.value);
            }
          }
          else if ((processChunkedState == null) && header.name.equalsIgnoreCase("Content-Length") && !header.value.isEmpty())
          {
            try
            {
              contentLength = Long.parseLong(header.value);
            }
            catch (NumberFormatException e)
            {
              throw new HttpProtocolException(currentMessage, "Invalid Content-Length header: \"" + header.value + "\"");
            }
          }
        }
      }
      else
      {
        onHeadersProcessed();

        if ((processChunkedState != null))
        {
          readState = ReadState.READING_CHUNKED_CONTENT;
        }
        else if (contentLength != null && contentLength > 0)
        {
          contentRead = 0;
          readState = ReadState.READING_FIXED_LENGTH_CONTENT;
        }
        else
        {
          readState = ReadState.DONE;
        }
        break;
      }
    }
  }

  void readFixedWidthContent()
  {
    contentRead += readBuffer.remaining();
    readBuffer.position(readBuffer.limit());
    if (contentRead >= contentLength)
    {
      readState = ReadState.DONE;
    }
  }

  void processChunkedData() throws HttpProtocolException
  {
    byte[] bytes;
    String line;
    while (readBuffer.hasRemaining())
    {
      switch (processChunkedState)
      {
        case READ_BYTE_COUNT:
          bytes = readNextLine(false);
          if (bytes != null)
          {
            line = new String(bytes);
            try
            {
              chunkedNextBytesTotal = Integer.parseInt(line.trim(), 16);
              if (chunkedNextBytesTotal == 0)
              {
                processChunkedState = ReadChunkedInputState.READ_LAST_LINE;
              }
              else if (chunkedNextBytesTotal < 0)
              {
                throw new HttpProtocolException(currentMessage, "Invalid chunked encoding byte count: " + chunkedNextBytesTotal);
              }
              else
              {
                chunkedNextBytesRead = 0;
                processChunkedState = ReadChunkedInputState.READ_BYTES;
              }
            }
            catch (Exception e)
            {
              throw new HttpProtocolException(currentMessage, "Invalid chunked encoding byte count: " + line);
            }
          }
          break;

        case READ_BYTES:
          int remainingBytesToRead = chunkedNextBytesTotal - chunkedNextBytesRead;
          int readBufferPosition = readBuffer.position();
          int remainingBytesInBuffer = readBuffer.limit() - readBufferPosition;

          if (remainingBytesInBuffer >= remainingBytesToRead)
          {
            readBuffer.position(readBufferPosition + remainingBytesToRead);
            chunkedNextBytesTotal = chunkedNextBytesRead = 0;
            processChunkedState = ReadChunkedInputState.CLEAR_READ_BYTES_TERMINATOR;
          }
          else
          {
            readBuffer.position(readBufferPosition + remainingBytesInBuffer);
            chunkedNextBytesRead += remainingBytesInBuffer;
          }
          break;

        case CLEAR_READ_BYTES_TERMINATOR:
          if (readNextLine(false) != null)
          {
            processChunkedState = ReadChunkedInputState.READ_BYTE_COUNT;
          }
          break;

        case READ_LAST_LINE:
          if (readNextLine(false) != null)
          {
            processChunkedState = null;
            readState = ReadState.DONE;
            return;
          }
          else
          {
            break;
          }
      }
    }
  }


  void reset()
  {
    readState = ReadState.READING_STATUS;
    currentLine.clear();
    if (writeBuffer != null)
    {
      writeBuffer.clear();
    }
    contentLength = null;
    contentRead = 0;
    processChunkedState = null;
  }

  protected byte[] readNextLine(boolean markReadBuffer)
  {
    while (readBuffer.hasRemaining())
    {
      byte b = readBuffer.get();
      currentLine.add(b);
      if (markReadBuffer)
      {
        readBuffer.mark();
      }
      if (b == LF)
      {
        byte[] lineBytes = currentLine.toArray();
        currentLine.clear();
        return lineBytes;
      }
    }

    return null;
  }

  static boolean lineHasContent(byte[] line)
  {
    for (int i = 0; i < line.length; i++)
    {
      byte b = line[i];
      if ((b != SPACE) && (b != CR) && (b != LF))
      {
        return true;
      }
    }
    return false;
  }

  enum ReadState
  {
    READING_STATUS,
    READING_HEADER,
    READING_FIXED_LENGTH_CONTENT,
    READING_CHUNKED_CONTENT,
    DONE;
  }
}

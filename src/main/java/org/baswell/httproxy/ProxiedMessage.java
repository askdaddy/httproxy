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
import java.util.Arrays;
import java.util.List;

import static org.baswell.httproxy.Header.*;
import static org.baswell.httproxy.Constants.*;

abstract class ProxiedMessage implements ProxiedRequest, ProxiedResponse
{
  abstract protected boolean write() throws ProxiedIOException;

  abstract protected ProxiedRequest proxiedRequest();

  abstract protected ProxiedResponse proxiedResponse();

  private long startedAt = System.currentTimeMillis();

  private long endedAt;

  private String method;

  private String path;

  private String version;

  private Integer status;

  private String reason;

  private String host;

  private Object attachment;

  protected final boolean request;

  protected final boolean overSsl;

  protected final ProxyDirector proxyDirector;

  protected ByteBuffer readBuffer;

  protected final int bufferSize;

  ReadState readState = ReadState.READING_STATUS;

  protected TByteArrayList currentLine = new TByteArrayList(50);

  protected TByteList writeBuffer;

  protected Long contentLength;

  protected long contentRead;

  protected ChunkedTerminatorState chunkedTerminatorState;

  protected ProxiedMessage(boolean request, boolean overSsl, ProxyDirector proxyDirector)
  {
    this.request = request;
    this.overSsl = overSsl;
    this.proxyDirector = proxyDirector;
    this.bufferSize = proxyDirector.getBufferSize();

    readBuffer = ByteBuffer.allocate(bufferSize);
    readBuffer.compact();
  }

  @Override
  public long startedAt()
  {
    return startedAt;
  }

  @Override
  public long endedAt()
  {
    return endedAt;
  }

  @Override
  public boolean overSSL()
  {
    return overSsl;
  }

  @Override
  public String host()
  {
    return host;
  }

  @Override
  public String method()
  {
    return method;
  }

  @Override
  public String path()
  {
    return path;
  }

  @Override
  public Object attachment()
  {
    return attachment;
  }

  @Override
  public void attach(Object attachment)
  {
    this.attachment = attachment;
  }

  @Override
  public int status()
  {
    return status;
  }

  @Override
  public String reason()
  {
    return reason;
  }

  boolean isMessageComplete()
  {
    return (readState == ReadState.DONE) && ((writeBuffer == null) || writeBuffer.isEmpty());
  }

  protected boolean readAndWriteBuffer() throws ProxiedIOException, IOException, HttpProtocolException, EndProxiedRequestException
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
          readChunkedContent();
          break;

        case DONE:
          break READ_STATE_LOOP;
      }
    }

    if (readState == ReadState.DONE)
    {
      endedAt = System.currentTimeMillis();
      if (!request)
      {
        proxyDirector.onExchangeComplete(proxiedRequest(), proxiedResponse());
      }
    }

    return write();
  }

  void readStatusLine() throws IOException
  {
    byte[] statusLine = readNextLine();
    if (statusLine != null)
    {
      String[] values = new String(statusLine).trim().split(" ");
      if (request)
      {
        for (int i = 0; i < values.length; i++)
        {
          String value = values[i].trim();

          if (!value.isEmpty())
          {
            if (method == null)
            {
              method = value;
            }
            else if (path == null)
            {
              path = value;
            }
            else
            {
              version = value;
              break;
            }
          }
        }
      }
      else
      {
        for (int i = 0; i < values.length; i++)
        {
          String value = values[i].trim();

          if (!value.isEmpty())
          {
            if (version == null)
            {
              version = value;
            }
            else if (status == null)
            {
              try
              {
                status = Integer.parseInt(value);
              }
              catch (NumberFormatException e)
              {
                status = -1;
              }
            }
            else
            {
              reason = value;
              break;
            }
          }
        }
      }

      readState = ReadState.READING_HEADER;
    }
  }

  void readHeaderLine() throws HttpProtocolException, IOException, EndProxiedRequestException
  {
    byte[] headerLineBytes;
    while ((headerLineBytes = readNextLine()) != null)
    {
      if (lineHasContent(headerLineBytes))
      {
        String headerLine = new String(headerLineBytes).trim();
        int index = headerLine.indexOf(':');
        if ((index > 0) && (index < (headerLine.length() - 1)))
        {
          String headerName = headerLine.substring(0, index).trim();
          String headerValue = headerLine.substring(index + 1, headerLine.length()).trim();

          /*
           * The latest HTTP specification defines only one transfer-encoding, chunked encoding
           *
           * If a message is received with both Content-Length and Transfer-Encoding the Content-Length header
           * must be ignored because the transfer encoding will change the way entity bodies are represented and
           * transferred (and number of bytes transmitted).
           */
          if (headerName.equalsIgnoreCase("Transfer-Encoding") )
          {
            if (headerValue.equalsIgnoreCase("chunked"))
            {
              chunkedTerminatorState = ChunkedTerminatorState.START;
            }
            else
            {
              throw new HttpProtocolException(request, "Don't know how to transfer encoding: " + headerValue);
            }
          }
          else if ((chunkedTerminatorState == null) && headerName.equalsIgnoreCase("Content-Length") && !headerValue.isEmpty())
          {
            try
            {
              contentLength = Long.parseLong(headerValue);
            }
            catch (NumberFormatException e)
            {
              throw new HttpProtocolException(request, "Invalid Content-Length header: \"" + headerValue + "\"");
            }
          }
          else if (headerName.equalsIgnoreCase("Host"))
          {
            host = headerValue;
          }

          if (proxyDirector != null)
          {
            String modifiedHeaderValue = request ? proxyDirector.siftRequestHeader(headerName, headerValue, proxiedRequest()) : proxyDirector.siftResponseHeader(headerName, headerValue, proxiedRequest(), proxiedResponse());
            if ((modifiedHeaderValue != null) && !modifiedHeaderValue.equals(headerValue))
            {
              insertIntoReadBuffer(toHeaderLines(Arrays.asList(new Header(headerName, modifiedHeaderValue))), headerLineBytes.length);
            }
          }
        }
      }
      else
      {
        /*
         * If this is the first request for this channel we need to used the processed headers to determine the endpoint.
         */
        onHeadersProcessed();

        if (proxyDirector != null)
        {
          List<Header> addedHeaders = request ? proxyDirector.addRequestHeaders(proxiedRequest()) : proxyDirector.addResponseHeaders(proxiedRequest(), proxiedResponse());
          if ((addedHeaders != null) && !addedHeaders.isEmpty())
          {
            insertIntoReadBuffer(toHeaderLines(addedHeaders, headerLineBytes), headerLineBytes.length);
          }
        }

        if ((chunkedTerminatorState != null))
        {
          readState = ReadState.READING_CHUNKED_CONTENT;
        }
        else if (contentLength != null)
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

  void readChunkedContent()
  {
    while (readBuffer.hasRemaining())
    {
      byte b = readBuffer.get();
      switch (chunkedTerminatorState)
      {
        case START:
          if (b == CR)
          {
            chunkedTerminatorState = ChunkedTerminatorState.CR_1;
          }
          break;

        case CR_1:
          chunkedTerminatorState = b == LF ? ChunkedTerminatorState.LF_1 : ChunkedTerminatorState.START;
          break;

        case LF_1:
          chunkedTerminatorState = b == CR ? ChunkedTerminatorState.CR_2 : ChunkedTerminatorState.START;
          break;

        case CR_2:
          if (b == LF)
          {
            chunkedTerminatorState = ChunkedTerminatorState.LF_2;
            readState = ReadState.DONE;
            return;
          }
          else
          {
            chunkedTerminatorState = ChunkedTerminatorState.START;
            break;
          }
      }
    }
  }
  /*
 * TODO How slow is this ?
 */
  void insertIntoReadBuffer(byte[] insertedBytes, Integer replaceLastBytes)
  {
    int currentPosition = readBuffer.position();
    int currentLimit = readBuffer.limit();

    readBuffer.reset();
    if (replaceLastBytes == null)
    {
      readBuffer.limit(currentPosition);
    }
    else
    {
      readBuffer.limit(currentPosition - replaceLastBytes);
    }

    int newBufferSize = Math.max(bufferSize, currentLimit + insertedBytes.length);
    ByteBuffer newBuffer = ByteBuffer.allocate(newBufferSize);
    newBuffer.mark();
    newBuffer.put(readBuffer);
    newBuffer.put(insertedBytes);

    readBuffer.limit(currentLimit);
    readBuffer.position(currentPosition);

    while (readBuffer.hasRemaining())
    {
      newBuffer.put(readBuffer);
    }

    if (replaceLastBytes == null)
    {
      newBuffer.position(currentPosition + insertedBytes.length);
      newBuffer.limit(currentLimit + insertedBytes.length);
    }
    else
    {
      newBuffer.position(currentPosition + insertedBytes.length - replaceLastBytes);
      newBuffer.limit(currentLimit + insertedBytes.length - replaceLastBytes);
    }

    readBuffer = newBuffer;
  }

  protected byte[] readNextLine()
  {
    while (readBuffer.hasRemaining())
    {
      byte b = readBuffer.get();
      currentLine.add(b);
      if (b == LF)
      {
        byte[] lineBytes = currentLine.toArray();
        currentLine.clear();
        return lineBytes;
      }
    }

    return null;
  }

  protected void onHeadersProcessed() throws IOException, EndProxiedRequestException
  {}

  protected void reset()
  {
    startedAt = System.currentTimeMillis();
    endedAt = 0;

    method = null;
    path = null;
    version = null;
    status = null;
    reason = null;
    host = null;

    readState = ReadState.READING_STATUS;
    currentLine.clear();
    if (writeBuffer != null)
    {
      writeBuffer.clear();
    }
    contentLength = null;
    contentRead = 0;
    chunkedTerminatorState = null;
  }

  protected static boolean lineHasContent(byte[] line)
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

  protected static byte[] toHeaderLines(List<Header> headers)
  {
    return toHeaderLines(headers, null);
  }

  protected static byte[] toHeaderLines(List<Header> headers, byte[] appenededBytes)
  {
    TByteArrayList bytes = new TByteArrayList(headers.size() * 50);
    for (Header header : headers)
    {
      bytes.add((header.name + ": " + header.value).getBytes());
      bytes.add(CR);
      bytes.add(LF);
    }
    if (appenededBytes != null)
    {
      bytes.addAll(appenededBytes);
    }
    return bytes.toArray();
  }

  static enum ReadState
  {
    READING_STATUS,
    READING_HEADER,
    READING_FIXED_LENGTH_CONTENT,
    READING_CHUNKED_CONTENT,
    DONE;
  }

  protected static enum ChunkedTerminatorState
  {
    START,
    CR_1,
    LF_1,
    CR_2,
    LF_2;
  }
}

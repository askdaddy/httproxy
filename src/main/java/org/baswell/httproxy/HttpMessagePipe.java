package org.baswell.httproxy;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.baswell.httproxy.Constants.*;

abstract class HttpMessagePipe
{
  abstract void readStatusLine() throws IOException;

  abstract void onHeadersProcessed() throws IOException, EndProxiedRequestException;

  abstract protected boolean write() throws ProxiedIOException;

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

  ChunkedTerminatorState chunkedTerminatorState;

  HttpMessagePipe(ProxyDirector proxyDirector)
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
          readChunkedContent();
          break;

        case DONE:
          break READ_STATE_LOOP;
      }
    }

    if (readState == ReadState.DONE)
    {
      onMessageDone();
    }

    return write();
  }

  boolean isMessageComplete()
  {
    return (readState == ReadState.DONE) && ((writeBuffer == null) || writeBuffer.isEmpty());
  }

  void readHeaderLine() throws HttpProtocolException, IOException, EndProxiedRequestException
  {
    byte[] headerLineBytes;
    while ((headerLineBytes = readNextLine()) != null)
    {
      if (lineHasContent(headerLineBytes))
      {
        Header header = currentMessage.addHeader(new String(headerLineBytes).trim());
        if (header != null)
        {
          /*
           * The latest HTTP specification defines only one transfer-encoding, chunked encoding
           *
           * If a message is received with both Content-Length and Transfer-Encoding the Content-Length header
           * must be ignored because the transfer encoding will change the way entity bodies are represented and
           * transferred (and number of bytes transmitted).
           */
          if (header.name.equalsIgnoreCase("Transfer-Encoding") )
          {
            if (header.value.equalsIgnoreCase("chunked"))
            {
              chunkedTerminatorState = ChunkedTerminatorState.START;
            }
            else
            {
              throw new HttpProtocolException(currentMessage, "Don't know how to transfer encoding: " + header.value);
            }
          }
          else if ((chunkedTerminatorState == null) && header.name.equalsIgnoreCase("Content-Length") && !header.value.isEmpty())
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
    chunkedTerminatorState = null;
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

  enum ChunkedTerminatorState
  {
    START,
    CR_1,
    LF_1,
    CR_2,
    LF_2;
  }
}

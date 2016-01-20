package org.baswell.httproxy;

import gnu.trove.list.array.TByteArrayList;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.baswell.httproxy.Constants.*;

class ModifiedOutputStream extends OutputStream implements ModifiedOutputBridge
{
  private final HttpResponse response;

  private final ResponseContentModifier modifier;

  private final OutputStream outputStream;

  private final int minimumResponseChunkSize;

  private final boolean inputChunked;

  private ReadChunkedInputState chunkedInputState;

  private int chunkedLineRemaining;

  private TByteArrayList inputBuffer;

  private final TByteArrayList outputBuffer = new TByteArrayList();

  ModifiedOutputStream(HttpResponse response, ResponseContentModifier modifier, OutputStream outputStream, int minimumResponseChunkSize)
  {
    this.response = response;
    this.modifier = modifier;
    this.outputStream = outputStream;
    this.minimumResponseChunkSize = minimumResponseChunkSize;

    if ("chunked".equalsIgnoreCase(response.getHeaderValue("Transfer-Encoding")))
    {
      inputChunked = true;
      chunkedInputState = ReadChunkedInputState.READ_BYTE_COUNT;
    }
    else
    {
      inputChunked = false;
      inputBuffer = new TByteArrayList();

      /*
       * Since the response is being modified we don't know the final length. Will have to send this back chunked.
       */
      response.removeHeader("Content-Length");
      response.setOrAddHeader("Transfer-Encoding", "chunked");
    }

  }

  public void done() throws IOException
  {
    modifier.complete(this);

    writeBuffer();

    // Chunked encoding body complete.
    outputStream.write("0".getBytes());
    outputStream.write(Constants.CR);
    outputStream.write(Constants.LF);
    outputStream.write(Constants.CR);
    outputStream.write(Constants.LF);
  }

  @Override
  public void write(int i) throws IOException
  {
    procesInput(new byte[]{(byte) i});
  }

  @Override
  public void write(byte[] bytes) throws IOException
  {
    procesInput(bytes);
  }

  @Override
  public void write(byte[] bytes, int offset, int length) throws IOException
  {
    procesInput(Arrays.copyOfRange(bytes, offset, (offset + length)));
  }

  @Override
  public void push(byte[] bytes) throws IOException
  {
    outputBuffer.add(bytes);
    if (outputBuffer.size() >= minimumResponseChunkSize)
    {
      writeBuffer();
    }
  }

  void procesInput(byte[] bytes) throws IOException
  {
    if (!inputChunked)
    {
      modifier.push(bytes, this);
    }
    else
    {
      inputBuffer.add(bytes);
      boolean needMoreBytes = false;

      while (!inputBuffer.isEmpty() && !needMoreBytes)
      {
        byte[] lineBytes;
        String line;
        switch (chunkedInputState)
        {
          case READ_BYTE_COUNT:
            lineBytes = readNextInputLine();
            if (lineBytes != null)
            {
              line = new String(bytes);
              try
              {
                chunkedLineRemaining = Integer.parseInt(line.trim(), 16);
                if (chunkedLineRemaining == 0)
                {
                  chunkedInputState = ReadChunkedInputState.READ_LAST_LINE;
                }
                else if (chunkedLineRemaining < 0)
                {
                  throw new IOException("Invalid chunked encoding byte count: " + chunkedLineRemaining);
                }
                else
                {
                  chunkedInputState = ReadChunkedInputState.READ_BYTES;
                }
              }
              catch (Exception e)
              {
                throw new IOException("Invalid chunked encoding byte count: " + line);
              }

            }
            else
            {
              needMoreBytes = true;
            }
            break;

          case READ_BYTES:
            int bytesToRead = Math.min(chunkedLineRemaining, inputBuffer.size());

            modifier.push(inputBuffer.toArray(0, bytesToRead), this);
            inputBuffer.remove(0, bytesToRead);

            chunkedLineRemaining -= bytesToRead;
            if (chunkedLineRemaining == 0)
            {
              chunkedInputState = ReadChunkedInputState.CLEAR_READ_BYTES_TERMINATOR;
            }
            break;

          case CLEAR_READ_BYTES_TERMINATOR:
            if (readNextInputLine() == null)
            {
              needMoreBytes = true;
            }
            else
            {
              chunkedInputState = ReadChunkedInputState.READ_BYTE_COUNT;
            }
            break;

          case READ_LAST_LINE:
            // No more application data
            break;
        }
      }
    }
  }

  void writeBuffer() throws IOException
  {
    if (!outputBuffer.isEmpty())
    {
      outputStream.write(Integer.toString(outputBuffer.size(), 16).getBytes());
      outputStream.write(Constants.CR);
      outputStream.write(Constants.LF);
      outputStream.write(outputBuffer.toArray());
      outputStream.write(Constants.CR);
      outputStream.write(Constants.LF);

      outputBuffer.clear();
    }
  }

  byte[] readNextInputLine()
  {
    for (int i = 0; i < inputBuffer.size(); i++)
    {
      if (inputBuffer.get(i) == LF)
      {
        byte[] line = inputBuffer.subList(0, i + 1).toArray();
        inputBuffer.remove(0, i + 1);
        return line;
      }
    }

    return null;
  }
}

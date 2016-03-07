package org.baswell.httproxy;

import gnu.trove.list.array.TByteArrayList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static org.baswell.httproxy.Constants.*;
import static org.baswell.httproxy.SharedMethods.*;
/**
 * WARNING: You're about to experience the worst code in the project.
 */
class ModifiedOutputStream extends OutputStream
{
  private final HttpRequest request;

  private final HttpResponse response;

  private final Charset charset;

  private final boolean requestModifier;

  private final RequestContentModifier requestContentModifier;

  private final ResponseContentModifier responseContentModifier;

  private final OutputStream outputStream;

  private final int minimumChunkSize;

  private final ProxyLogger log;

  private final boolean inputChunked;

  private ReadChunkedInputState chunkedInputState;

  private int chunkedLineRemaining;

  private TByteArrayList inputBuffer;

  private final TByteArrayList outputBuffer = new TByteArrayList();

  private OutputStreamBridge outputStreamBridge = new OutputStreamBridge();

  private final ContentEncoderDecoder encoderDecoder;

  ModifiedOutputStream(HttpRequest request, RequestContentModifier requestContentModifier, OutputStream outputStream, int minimumChunkSize, ProxyLogger log) throws IOException
  {
    this(request, null, requestContentModifier, null, outputStream, minimumChunkSize, log);
  }

  ModifiedOutputStream(HttpRequest request, HttpResponse response, ResponseContentModifier responseContentModifier, OutputStream outputStream, int minimumChunkSize, ProxyLogger log) throws IOException
  {
    this(request, response, null, responseContentModifier, outputStream, minimumChunkSize, log);
  }

  private ModifiedOutputStream(HttpRequest request, HttpResponse response, RequestContentModifier requestContentModifier, ResponseContentModifier responseContentModifier, OutputStream outputStream, int minimumChunkSize, ProxyLogger log) throws IOException
  {
    this.request = request;
    this.response = response;
    this.requestContentModifier = requestContentModifier;
    this.responseContentModifier = responseContentModifier;
    this.outputStream = outputStream;
    this.minimumChunkSize = minimumChunkSize;
    this.log = log;

    requestModifier = requestContentModifier != null;

    HttpMessage contentMessage = response == null ? request : response;

    if ("chunked".equalsIgnoreCase(contentMessage.getHeaderValue("Transfer-Encoding")))
    {
      inputChunked = true;
      inputBuffer = new TByteArrayList();
      chunkedInputState = ReadChunkedInputState.READ_BYTE_COUNT;
    }
    else
    {
      inputChunked = false;

      /*
       * Since the response is being modified we don't know the final length. Will have to send this back chunked.
       */
      contentMessage.removeHeader("Content-Length");
      contentMessage.setOrAddHeader("Transfer-Encoding", "chunked");
    }

    String contentEncoding = contentMessage.getHeaderValue("Content-Encoding");
    if (contentEncoding == null || contentEncoding.equals("identity"))
    {
      encoderDecoder = new IdentityEncoderDecoder();
    }
    else if (contentEncoding.equalsIgnoreCase("gzip"))
    {
      encoderDecoder = new GzipEncoderDecoder();
    }
    else if (contentEncoding.equalsIgnoreCase("deflate"))
    {
      encoderDecoder = new DeflateEncoderDecoder();
    }
    else
    {
      log.warn("Unsupported Content-Encoding: " + contentEncoding + ". Defaulting to identity Content-Encoding.");
      encoderDecoder = new IdentityEncoderDecoder();
    }

    charset = contentTypeToCharset(contentMessage.getHeaderValue("Content-Type"), log);
  }


  /**
   * Called when all bytes from proxied server have been received.
   */
  public void onContentComplete() throws IOException
  {
    byte[] bytes = encoderDecoder.inputDone();
    if (bytes != null && bytes.length > 0)
    {
      if (requestModifier)
      {
        requestContentModifier.modifyAndWrite(request, bytes, charset, outputStreamBridge);
      }
      else
      {
        responseContentModifier.modifyAndWrite(request, response, bytes, charset, outputStreamBridge);
      }
    }

    if (requestModifier)
    {
      requestContentModifier.requestComplete(request, charset, outputStreamBridge);
    }
    else
    {
      responseContentModifier.responseComplete(request, response, charset, outputStreamBridge);
    }

    bytes = encoderDecoder.finishOutput();
    if (bytes != null && bytes.length > 0)
    {
      outputBuffer.add(bytes);
    }

    writeChunk();

    // Chunked encoding complete footer
    outputStream.write("0".getBytes());
    outputStream.write(Constants.CR);
    outputStream.write(Constants.LF);
    outputStream.write(Constants.CR);
    outputStream.write(Constants.LF);
  }

  @Override
  public void write(int i) throws IOException
  {
    onRawInput(new byte[]{(byte) i});
  }

  @Override
  public void write(byte[] bytes) throws IOException
  {
    onRawInput(bytes);
  }

  @Override
  public void write(byte[] bytes, int offset, int length) throws IOException
  {
    onRawInput(Arrays.copyOfRange(bytes, offset, (offset + length)));
  }

  @Override
  public void flush() throws IOException
  {
    outputStream.flush();
  }

  /**
   * Called from {@link PipedResponseStream} through InputStream API. These are the raw bytes (chunked encoded, and compressed - possible).
   */
  void onRawInput(byte[] bytes) throws IOException
  {
    if (!inputChunked)
    {
      bytes = encoderDecoder.decode(bytes);
      if (bytes != null && bytes.length > 0)
      {
        if (requestModifier)
        {
          requestContentModifier.modifyAndWrite(request, bytes, charset, outputStreamBridge);
        }
        else
        {
          responseContentModifier.modifyAndWrite(request, response, bytes, charset, outputStreamBridge);
        }
      }
    }
    else
    {
      inputBuffer.add(bytes);
      boolean needMoreBytes = false;

      while (!inputBuffer.isEmpty() && !needMoreBytes)
      {
        String line;
        switch (chunkedInputState)
        {
          case READ_BYTE_COUNT:
            byte[] lineBytes = readNextChunkedLine();
            if (lineBytes != null)
            {
              line = new String(lineBytes);
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
                throw new IOException("Invalid chunked encoding byte count: " + line, e);
              }

            }
            else
            {
              needMoreBytes = true;
            }
            break;

          case READ_BYTES:
            int bytesToRead = Math.min(chunkedLineRemaining, inputBuffer.size());

            byte[] uncompressedBytes = encoderDecoder.decode(inputBuffer.toArray(0, bytesToRead));
            inputBuffer.remove(0, bytesToRead);
            if (uncompressedBytes != null && uncompressedBytes.length > 0)
            {
              if (requestModifier)
              {
                requestContentModifier.modifyAndWrite(request, uncompressedBytes, charset, outputStreamBridge);
              }
              else
              {
                responseContentModifier.modifyAndWrite(request, response, uncompressedBytes, charset, outputStreamBridge);
              }
            }

            chunkedLineRemaining -= bytesToRead;
            if (chunkedLineRemaining == 0)
            {
              chunkedInputState = ReadChunkedInputState.CLEAR_READ_BYTES_TERMINATOR;
            }
            break;

          case CLEAR_READ_BYTES_TERMINATOR:
            if (readNextChunkedLine() == null)
            {
              needMoreBytes = true;
            }
            else
            {
              chunkedInputState = ReadChunkedInputState.READ_BYTE_COUNT;
            }
            break;

          case READ_LAST_LINE:
            inputBuffer.clear();
            // No more application data
            break;
        }
      }
    }
  }

  /**
   * Called from {@link ResponseContentModifier} as the content from the proxied server is modified.
   */
  void onContentModified(byte[] bytes) throws IOException
  {
    bytes = encoderDecoder.encode(bytes);
    if (bytes != null && bytes.length > 0)
    {
      outputBuffer.add(bytes);
      if (outputBuffer.size() >= minimumChunkSize)
      {
        writeChunk();
      }
    }
  }

  /**
   * Write the content of the output buffer as a chunked line to the client output stream.
   */
  void writeChunk() throws IOException
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

  /**
   * Reads the next line from the input buffer (all bytes to the first line feed character).
   */
  byte[] readNextChunkedLine()
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

  static Charset contentTypeToCharset(String contentType, ProxyLogger log)
  {
    if (nullEmpty(contentType))
    {
      return null;
    }
    else
    {
      int index = contentType.indexOf("charset=");
      if (index < 0)
      {
        return null;
      }
      else
      {
        String charSetString = contentType.substring(index + "charset=".length());
        index = charSetString.indexOf(";");
        if (index > 0)
        {
          charSetString = charSetString.substring(0, index);
        }

        try
        {
          return Charset.forName(charSetString.toUpperCase());
        }
        catch (Exception e)
        {
          if (log != null)
          {
            log.warn("Unsupported charset: " + charSetString + " in content type: " + contentType);
          }
          return null;
        }
      }
    }
  }

  /**
   * This is the OutputStream that gets sent to {@link ResponseContentModifier}.
   */
  class OutputStreamBridge extends OutputStream
  {
    @Override
    public void write(int i) throws IOException
    {
      onContentModified(new byte[]{(byte) i});
    }

    @Override
    public void write(byte[] bytes) throws IOException
    {
      onContentModified(bytes);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException
    {
      onContentModified(Arrays.copyOfRange(bytes, offset, (offset + length)));
    }
  }

  interface ContentEncoderDecoder
  {
    byte[] decode(byte[] encodedBytes) throws IOException;
    byte[] encode(byte[] decodedBytes) throws IOException;
    byte[] inputDone() throws IOException;
    byte[] finishOutput() throws IOException;
  }

  /**
   * Pass-through EncoderDecoder used when Content-Encoding = "identity", the Content-Encoding header isn't specified or the Content-Type is some
   * unsupported value.
   */
  class IdentityEncoderDecoder implements ContentEncoderDecoder
  {
    @Override
    public byte[] decode(byte[] encodedBytes) throws IOException
    {
      return encodedBytes;
    }

    @Override
    public byte[] encode(byte[] decodedBytes) throws IOException
    {
      return decodedBytes;
    }

    public byte[] inputDone()
    {
      return null;
    }

    @Override
    public byte[] finishOutput()
    {
      return null;
    }
  }

  /**
   * This sucks. Need to find a non-blocking GZip library to keep from having to spawn a thread. I think it can be done
   * with java.util.zip.Inflator but the headers and footers have to be processed manually.
   */
  abstract class StreamEncoderDecoder extends InputStream implements ContentEncoderDecoder, Runnable
  {
    abstract InputStream createDecompressingInputStream(InputStream inputStream) throws IOException;

    abstract OutputStream createCompressingOutputStream(OutputStream outputStream) throws IOException;

    final TByteArrayList encodedInputBuffer = new TByteArrayList();

    final TByteArrayList decodedInputBuffer = new TByteArrayList();

    final ByteArrayOutputStream encodedOutputBuffer = new ByteArrayOutputStream();

    final OutputStream encodingOutputStream;

    final Thread decoderThread;

    boolean threadRunning;

    boolean done = false;

    StreamEncoderDecoder() throws IOException
    {
      encodingOutputStream = createCompressingOutputStream(encodedOutputBuffer);

      decoderThread = new Thread(this, getClass().getSimpleName());
      decoderThread.start();
    }

    @Override
    public int available()
    {
      return encodedInputBuffer.size();
    }

    @Override
    public synchronized int read() throws IOException
    {
      if (done)
      {
        return -1;
      }

      if (encodedInputBuffer.isEmpty())
      {
        /*
         * No data yet so we need to wait. We'll be woken up in decode() when more bytes are added to the input buffer on in done() when
         * no data is left. Before we go to sleep need to notify the wait() in decode() so it can return the data decoded so far.
         */
        notify();
        try
        {
          wait();
        }
        catch (InterruptedException e)
        {}
      }

      if (done || encodedInputBuffer.isEmpty())
      {
        return -1;
      }
      else
      {
        byte b = encodedInputBuffer.get(0);
        encodedInputBuffer.removeAt(0);
        return b & 0x000000ff;
      }
    }

    @Override
    public synchronized byte[] decode(byte[] encodedBytes) throws IOException
    {
      encodedInputBuffer.add(encodedBytes);

      notify(); // Wake up decoderThread which is a sleep in read().
      try
      {
        wait(); // decoderThread will notify us in read() when all the input has been processed so we can return it here.
      }
      catch (InterruptedException e)
      {}

      if (decodedInputBuffer.isEmpty())
      {
        return null;
      }
      else
      {
        byte[] decompressedBytes = decodedInputBuffer.toArray();
        decodedInputBuffer.clear();
        return decompressedBytes;
      }
    }

    @Override
    public byte[] encode(byte[] decodedBytes) throws IOException
    {
      encodingOutputStream.write(decodedBytes);

      if (encodedOutputBuffer.size() > 0)
      {
        byte[] compressedBytes = encodedOutputBuffer.toByteArray();
        encodedOutputBuffer.reset();
        return compressedBytes;
      }
      else
      {
        return null;
      }
    }

    @Override
    public void run()
    {
      threadRunning = true;
      try
      {
        InputStream decodingInputStream = createDecompressingInputStream(this);

        int i;
        while ((i = decodingInputStream.read()) != -1)
        {
          decodedInputBuffer.add((byte) i);
        }
      }
      catch (IOException e)
      {
        // Since the input stream is sourced by encodedInputBuffer we *should* never get here.
        log.error(getClass().getSimpleName() + " IOException on decoded read.", e);
      }

      synchronized (this)
      {
        notify(); // Notify inputDone() that all bytes have been processed
        threadRunning = false;
      }
    }

    public synchronized byte[] inputDone()
    {
      if (threadRunning)
      {
        done = true;
        notify(); // Wake up decoderThread which is a sleep in read().
        try
        {
          wait(); // decoderThread will notify us at the end of run() when all the input has been processed so we can return it here.
        }
        catch (InterruptedException e)
        {
        }
      }

      return decodedInputBuffer.toArray();
    }


    @Override
    public byte[] finishOutput() throws IOException
    {
      if (encodingOutputStream instanceof DeflaterOutputStream)
      {
        ((DeflaterOutputStream) encodingOutputStream).finish();
      }

      return encodedOutputBuffer.size() == 0 ? null : encodedOutputBuffer.toByteArray();
    }
  }

  class GzipEncoderDecoder extends StreamEncoderDecoder
  {
    GzipEncoderDecoder() throws IOException
    {}

    @Override
    InputStream createDecompressingInputStream(InputStream inputStream) throws IOException
    {
      return new GZIPInputStream(inputStream);
    }

    @Override
    OutputStream createCompressingOutputStream(OutputStream outputStream) throws IOException
    {
      return new GZIPOutputStream(outputStream);
    }
  }

  class DeflateEncoderDecoder extends StreamEncoderDecoder
  {
    DeflateEncoderDecoder() throws IOException
    {}

    @Override
    InputStream createDecompressingInputStream(InputStream inputStream) throws IOException
    {
      return new InflaterInputStream(inputStream);
    }

    @Override
    OutputStream createCompressingOutputStream(OutputStream outputStream) throws IOException
    {
      return new DeflaterOutputStream(outputStream);
    }
  }

  /*
   * Testing purposes only
   */
  class IdentityEncoderDecoderStream extends StreamEncoderDecoder
  {
    IdentityEncoderDecoderStream() throws IOException
    {}

    @Override
    InputStream createDecompressingInputStream(InputStream inputStream)
    {
      return inputStream;
    }

    @Override
    OutputStream createCompressingOutputStream(OutputStream outputStream)
    {
      return outputStream;
    }
  }

  /**
   * @deprecated This doesn't work for GZip.
   */
  class InflatorDeflator implements ContentEncoderDecoder
  {
    final Inflater inflater;

    final Deflater deflater;

    public InflatorDeflator()
    {
      inflater = new Inflater(true);
      deflater = new Deflater(5, true);
    }

    @Override
    public byte[] decode(byte[] encodedBytes) throws IOException
    {
      inflater.setInput(encodedBytes);

      try
      {
        TByteArrayList decoded = new TByteArrayList();
        byte[] buffer = new byte[1024];
        int inflated;
        while ((inflated = inflater.inflate(buffer)) > 0)
        {
          decoded.add(buffer, 0, inflated);
        }

        return decoded.isEmpty() ? null : decoded.toArray();
      }
      catch (DataFormatException e)
      {
        throw new IOException(e);
      }
    }

    @Override
    public byte[] encode(byte[] decodedBytes) throws IOException
    {
      deflater.setInput(decodedBytes);

      TByteArrayList encoded = new TByteArrayList();
      byte[] buffer = new byte[1024];
      int inflated;
      while ((inflated = deflater.deflate(buffer)) > 0)
      {
        encoded.add(buffer, 0, inflated);
      }

      return encoded.isEmpty() ? null : encoded.toArray();
    }

    @Override
    public byte[] inputDone() throws IOException
    {
      try
      {
        TByteArrayList decoded = new TByteArrayList();
        byte[] buffer = new byte[1024];
        int inflated;
        while ((inflated = inflater.inflate(buffer)) > 0)
        {
          decoded.add(buffer, 0, inflated);
        }

        return decoded.isEmpty() ? null : decoded.toArray();
      }
      catch (DataFormatException e)
      {
        throw new IOException(e);
      }
    }

    @Override
    public byte[] finishOutput() throws IOException
    {
      TByteArrayList encoded = new TByteArrayList();
      byte[] buffer = new byte[1024];
      int inflated;
      while ((inflated = deflater.deflate(buffer)) > 0)
      {
        encoded.add(buffer, 0, inflated);
      }

      return encoded.isEmpty() ? null : encoded.toArray();
    }
  }
}

package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

class ClientOutputStream extends OutputStream
{
  final OutputStream rawOutputStream;

  OutputStream wrappedOutputStream;

  ClientOutputStream(OutputStream rawOutputStream)
  {
    this.rawOutputStream = rawOutputStream;
  }

  @Override
  public void write(int i) throws IOException
  {
    if (wrappedOutputStream == null)
    {
      rawOutputStream.write(i);
    }
    else
    {
      wrappedOutputStream.write(i);
    }
  }

  @Override
  public void write(byte[] bytes) throws IOException
  {
    if (wrappedOutputStream == null)
    {
      rawOutputStream.write(bytes);
    }
    else
    {
      wrappedOutputStream.write(bytes);
    }
  }

  @Override
  public void write(byte[] bytes, int offset, int length) throws IOException
  {
    if (wrappedOutputStream == null)
    {
      rawOutputStream.write(bytes, offset, length);
    }
    else
    {
      wrappedOutputStream.write(bytes, offset, length);
    }
  }
}

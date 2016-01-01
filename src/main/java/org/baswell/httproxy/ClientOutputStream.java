package org.baswell.httproxy;

import java.io.IOException;
import java.io.OutputStream;

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
}

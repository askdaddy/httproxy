package org.baswell.httproxy;

import java.io.IOException;

class ProxiedIOException extends Exception
{
  final boolean request;

  final IOException e;

  ProxiedIOException(boolean request, IOException e)
  {
    this.request = request;
    this.e = e;
  }
}

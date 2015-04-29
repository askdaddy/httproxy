package org.baswell.httproxy;

class HttpProtocolException extends Exception
{
  final boolean request;

  HttpProtocolException(boolean request, String message)
  {
    super(message);
    this.request = request;
  }
}

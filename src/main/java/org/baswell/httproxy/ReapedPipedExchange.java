package org.baswell.httproxy;

interface ReapedPipedExchange
{
  boolean active();

  Integer getLastKeepAliveTimeoutSeconds();

  long getLastExchangeAt();

  boolean closed();

  void close();
}

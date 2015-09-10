package org.baswell.httproxy;

interface ReapedPipedExchange
{
  boolean active();

  Integer getLastKeepAliveTimeoutSeconds();

  long getLastExchangeAt();

  void close();
}

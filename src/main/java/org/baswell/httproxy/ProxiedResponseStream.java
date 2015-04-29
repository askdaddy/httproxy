package org.baswell.httproxy;

import java.io.InputStream;
import java.io.OutputStream;

class ProxiedResponseStream extends ProxiedMessageStream
{
  ProxiedResponseStream(ProxiedExchangeStream proxiedExchangeStream, InputStream inputStream, OutputStream outputStream, IOProxyDirector proxyDirector)
  {
    super(false, proxiedExchangeStream, inputStream, outputStream, proxyDirector);
  }

  @Override
  protected ProxiedRequest proxiedRequest()
  {
    return proxiedExchangeStream.requestStream;
  }

  @Override
  protected ProxiedResponse proxiedResponse()
  {
    return this;
  }
}

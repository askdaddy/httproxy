package org.baswell.httproxy;

import java.io.IOException;
import java.io.InputStream;

class ProxiedRequestStream extends ProxiedMessageStream
{
  ProxiedRequestStream(ProxiedExchangeStream proxiedStream, InputStream inputStream, IOProxyDirector proxyDirector)
  {
    super(true, proxiedStream, inputStream, proxyDirector);
  }

  @Override
  protected ProxiedRequest proxiedRequest()
  {
    return this;
  }

  @Override
  protected ProxiedResponse proxiedResponse()
  {
    return proxiedExchangeStream.responseStream;
  }

  @Override
  protected void onHeadersProcessed() throws IOException
  {
    super.onHeadersProcessed();
    if (outputStream == null)
    {
      outputStream = proxiedExchangeStream.connectProxiedServer();
    }
  }
}

/*
 * Copyright 2015 Corey Baswell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.baswell.httproxy;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static org.baswell.httproxy.PipedMessageStreamMethods.*;

class PipedRequestStream extends PipedRequest
{
  private final PipedExchangeStream exchangeStream;

  private final InputStream inputStream;

  private final boolean overSSL;

  private final byte[] readBytes;

  private final int sleepSecondsOnReadWait;

  OutputStream currentOutputStream;

  PipedRequestStream(IOProxyDirector proxyDirector, PipedExchangeStream exchangeStream, Socket clientSocket) throws IOException
  {
    super(proxyDirector);
    this.exchangeStream = exchangeStream;

    inputStream= clientSocket.getInputStream();
    overSSL = clientSocket instanceof SSLSocket;

    setClientIp(clientSocket);

    readBytes = new byte[bufferSize];
    sleepSecondsOnReadWait = proxyDirector.getSleepSecondsOnReadWait();
  }

  void readAndWriteMessage() throws ProxiedIOException, HttpProtocolException, EndProxiedRequestException
  {
    doReadAndWriteMessage(this, inputStream, readBytes, sleepSecondsOnReadWait);
  }

  @Override
  boolean write() throws ProxiedIOException
  {
    return doWrite(this, currentOutputStream, readBytes);
  }

  @Override
  void onRequest(HttpRequest request) throws EndProxiedRequestException, IOException
  {
    exchangeStream.onRequest();
    currentOutputStream.write(currentRequest.toBytes());
  }

  @Override
  void onMessageDone() throws IOException
  {}

  @Override
  public boolean overSSL()
  {
    return overSSL;
  }
}

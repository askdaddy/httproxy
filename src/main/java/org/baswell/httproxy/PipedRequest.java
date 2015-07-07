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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

abstract public class PipedRequest extends PipedMessage
{
  abstract boolean overSSL();

  abstract void onRequest(HttpRequest request) throws EndProxiedRequestException, IOException;

  HttpRequest currentRequest;

  boolean firstInExchange = true;

  String clientIp;

  PipedRequest(ProxyDirector proxyDirector)
  {
    super(proxyDirector);
  }

  @Override
  void readStatusLine() throws IOException
  {
    byte[] statusLine = readNextLine();
    if (statusLine != null)
    {
      readBuffer.mark();
      currentMessage = currentRequest = new HttpRequest(clientIp, firstInExchange, overSSL(), new String(statusLine).trim());
      firstInExchange = false;
      readState = ReadState.READING_HEADER;
    }
  }

  @Override
  void onHeadersProcessed() throws EndProxiedRequestException, IOException
  {
    onRequest(currentRequest);
  }

  void setClientIp(Socket socket)
  {
    clientIp = ((InetSocketAddress)socket.getRemoteSocketAddress()).getHostName();
  }
}

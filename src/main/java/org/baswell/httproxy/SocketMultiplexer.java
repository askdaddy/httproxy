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
import java.net.Socket;

class SocketMultiplexer extends ConnectionMultiplexer<Socket>
{
  @Override
  protected Socket connect(ConnectionParameters connectionParameters) throws IOException
  {
    Socket socket;
    if (connectionParameters.ssl)
    {
      socket = connectionParameters.sslContext.getSocketFactory().createSocket(connectionParameters.ipOrHost, connectionParameters.port);
    }
    else
    {
      socket = new Socket(connectionParameters.ipOrHost, connectionParameters.port);
    }

    return socket;
  }

  @Override
  protected void closeQuitely(Socket socket)
  {
    try
    {
      if (socket.isClosed())
      {
        socket.close();
      }
    }
    catch (IOException e)
    {}
  }
}

package org.baswell.httproxy;

import java.io.IOException;
import java.net.Socket;

class SocketMultiplexer extends ConnectionMultiplexer<Socket>
{
  @Override
  protected Socket connect(ConnectionParameters connectionParameters) throws IOException
  {
    if (connectionParameters.ssl)
    {
      return connectionParameters.sslContext.getSocketFactory().createSocket(connectionParameters.ipOrHost, connectionParameters.port);
    }
    else
    {
      return new Socket(connectionParameters.ipOrHost, connectionParameters.port);
    }
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

package org.baswell.httproxy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

abstract class ConnectionMultiplexer<Connection>
{
  abstract protected Connection connect(ConnectionParameters connectionParameters) throws IOException;

  abstract protected void closeQuitely(Connection connection);

  public Map<ConnectionParameters, Connection> connections = new HashMap<ConnectionParameters, Connection>();

  public Connection getConnectionFor(ConnectionParameters connectionParameters) throws IOException
  {
    if (connections.containsKey(connectionParameters))
    {
      return connections.get(connectionParameters);
    }
    else
    {
      Connection connection = connect(connectionParameters);
      connections.put(connectionParameters, connection);
      return connection;
    }
  }

  public void closeQuitely()
  {
    for (Connection connection : connections.values())
    {
      closeQuitely(connection);
    }
    connections.clear();
  }
}

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
import java.util.HashMap;
import java.util.Map;

abstract class ConnectionMultiplexer<Connection>
{
  abstract Connection connect(ConnectionParameters connectionParameters) throws IOException;

  abstract void closeQuitely(Connection connection);

  private Map<ConnectionParameters, Connection> connections = new HashMap<ConnectionParameters, Connection>();

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

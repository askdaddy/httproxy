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

import java.net.HttpCookie;

public class HttpResponse extends HttpMessage
{
  public String version;

  public int statusCode;

  public String reasonPhrase;

  public HttpResponse(String responseLine)
  {
    String[] values = new String(responseLine).trim().split(" ");
    for (int i = 0; i < values.length; i++)
    {
      String value = values[i].trim();

      if (!value.isEmpty())
      {
        if (version == null)
        {
          version = value;
        }
        else if (statusCode == 0)
        {
          try
          {
            statusCode = Integer.parseInt(value);
          }
          catch (NumberFormatException e)
          {
            statusCode = -1;
          }
        }
        else
        {
          if (reasonPhrase == null)
          {
            reasonPhrase = value;
          }
          else
          {
            reasonPhrase += " " + value;
          }
          break;
        }
      }
    }
  }

  @Override
  String getStatusLine()
  {
    return version + " " + statusCode + " " + reasonPhrase;
  }

  public void setCookie(HttpCookie cookie)
  {
    headers.add(new Header("Set-Cookie", cookie.toString()));
  }
}

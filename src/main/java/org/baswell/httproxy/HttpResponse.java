package org.baswell.httproxy;

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
}

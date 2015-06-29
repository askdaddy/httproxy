package org.baswell.httproxy;

public class HttpRequest extends HttpMessage
{
  public String method;

  public String path;

  public String version;

  public HttpRequest(String requestLine)
  {
    String[] values = new String(requestLine).trim().split(" ");
    for (int i = 0; i < values.length; i++)
    {
      String value = values[i].trim();

      if (!value.isEmpty())
      {
        if (method == null)
        {
          method = value;
        }
        else if (path == null)
        {
          path = value;
        }
        else
        {
          version = value;
          break;
        }
      }
    }
  }

  @Override
  String getStatusLine()
  {
    return method + " " + path + " " + version;
  }
}

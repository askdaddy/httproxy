package org.baswell.httproxy;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest
{
  public String url = "http://localhost:8081/test";

  public String method = "GET";

  public Map<String, String> headers = new HashMap<String, String>();

  public String content;
}

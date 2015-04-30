package org.baswell.httproxy;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse
{
  public int status;

  public String reason;

  public Map<String, String> headers = new HashMap<String, String>();

  public String content;
}

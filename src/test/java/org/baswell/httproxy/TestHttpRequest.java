package org.baswell.httproxy;

import java.util.HashMap;
import java.util.Map;

public class TestHttpRequest
{
  public String url = "http://localhost:9095/test";

  public String method = "GET";

  public Map<String, String> headers = new HashMap<String, String>();

  public String content;
}

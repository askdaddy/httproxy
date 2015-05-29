package org.baswell.httproxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

abstract public class TestProxyDirector implements ProxyDirector
{
  public int bufferSize = 1024 * 16;

  public Map<String, String> requestHeaders = new HashMap<String, String>();

  public Map<String, String> modifiedRequestHeaders = new HashMap<String, String>();

  public List<Header> addedRequestHeaders = new ArrayList<Header>();

  public Map<String, String> responseHeaders = new HashMap<String, String>();

  public Map<String, String> modifiedResponseHeaders = new HashMap<String, String>();

  public List<Header> addedResponseHeaders = new ArrayList<Header>();

  public boolean exchangeComplete;

  public String requestMethod;

  public String requestPath;

  public String host;

  public int responseCode;

  public String responseReason;

  public boolean requestProtocolError;

  public boolean responseProtocolError;

  public boolean prematureRequestClosed;

  public boolean prematureResponseClosed;


  public void reset()
  {
    requestHeaders.clear();
    modifiedRequestHeaders.clear();
    addedRequestHeaders.clear();
    responseHeaders.clear();
    modifiedResponseHeaders.clear();
    addedResponseHeaders.clear();

    requestMethod = null;
    requestPath = null;
    host = null;
    responseCode = 0;
    responseReason = null;

    exchangeComplete = false;
    requestProtocolError = false;
    responseProtocolError = false;
    prematureRequestClosed = false;
    prematureResponseClosed = false;
  }

  @Override
  public int getBufferSize()
  {
    return bufferSize;
  }

  @Override
  public String siftRequestHeader(String headerName, String headerValue, ProxiedRequest request)
  {
    requestHeaders.put(headerName, headerValue);
    return modifiedRequestHeaders.containsKey(headerName) ? modifiedRequestHeaders.get(headerName) : null;
  }

  @Override
  public List<Header> addRequestHeaders(ProxiedRequest request)
  {
    return addedRequestHeaders;
  }

  @Override
  public String siftResponseHeader(String headerName, String headerValue, ProxiedRequest request, ProxiedResponse response)
  {
    responseHeaders.put(headerName, headerValue);
    return modifiedResponseHeaders.containsKey(headerName) ? modifiedResponseHeaders.get(headerName) : null;
  }

  @Override
  public List<Header> addResponseHeaders(ProxiedRequest request, ProxiedResponse response)
  {
    return addedResponseHeaders;
  }

  @Override
  public void onExchangeComplete(ProxiedRequest request, ProxiedResponse response)
  {
    exchangeComplete = true;
    requestPath = request.path();
    host = request.host();
    requestMethod = request.method();
    responseCode = response.status();
    responseReason = response.reason();
  }

  @Override
  public void onRequestHttpProtocolError(ProxiedRequest request, String errorDescription)
  {
    requestProtocolError = true;
  }

  @Override
  public void onResponseHttpProtocolError(ProxiedRequest request, ProxiedResponse response, String errorDescription)
  {
    responseProtocolError = true;
  }

  @Override
  public void onPrematureRequestClosed(ProxiedRequest request, IOException e)
  {
    prematureRequestClosed = true;
  }

  @Override
  public void onPrematureResponseClosed(ProxiedRequest request, ProxiedResponse response, IOException e)
  {
    prematureResponseClosed = true;
  }

  @Override
  public ProxyLogger getLogger()
  {
    return new SimpleProxyLogger(SimpleProxyLogger.INFO_LEVEL);
  }
}

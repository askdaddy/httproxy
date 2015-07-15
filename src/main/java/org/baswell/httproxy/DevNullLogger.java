package org.baswell.httproxy;

class DevNullLogger implements ProxyLogger
{
  @Override
  public boolean logDebugs()
  {
    return false;
  }

  @Override
  public void debug(String message)
  {}

  @Override
  public void debug(String message, Throwable exception)
  {}

  @Override
  public boolean logInfos()
  {
    return false;
  }

  @Override
  public void info(String message)
  {}

  @Override
  public void info(String message, Throwable exception)
  {}

  @Override
  public boolean logWarns()
  {
    return false;
  }

  @Override
  public void warn(String message)
  {}

  @Override
  public void warn(String message, Throwable exception)
  {}

  @Override
  public boolean logErrors()
  {
    return false;
  }

  @Override
  public void error(String message)
  {}

  @Override
  public void error(String message, Throwable exception)
  {}
}

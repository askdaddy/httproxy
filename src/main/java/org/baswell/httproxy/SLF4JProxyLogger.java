package org.baswell.httproxy;

import org.slf4j.Logger;

/**
 * SLF4J logger bridge for the HttProxy runtime.
 */
public class SLF4JProxyLogger implements ProxyLogger
{
  private final Logger log;

  public SLF4JProxyLogger(Logger log)
  {
    this.log = log;
  }

  @Override
  public boolean logDebugs()
  {
    return log.isDebugEnabled();
  }

  @Override
  public void debug(String s)
  {
    log.debug(s);
  }

  @Override
  public void debug(String s, Throwable throwable)
  {
    log.debug(s, throwable);
  }

  @Override
  public boolean logInfos()
  {
    return log.isInfoEnabled();
  }

  @Override
  public void info(String s)
  {
    log.info(s);
  }

  @Override
  public void info(String s, Throwable throwable)
  {
    log.info(s, throwable);
  }

  @Override
  public boolean logWarns()
  {
    return log.isWarnEnabled();
  }

  @Override
  public void warn(String s)
  {
    log.warn(s);
  }

  @Override
  public void warn(String s, Throwable throwable)
  {
    log.warn(s, throwable);
  }

  @Override
  public boolean logErrors()
  {
    return log.isErrorEnabled();
  }

  @Override
  public void error(String s)
  {
    log.error(s);
  }

  @Override
  public void error(String s, Throwable throwable)
  {
    log.error(s, throwable);
  }
}

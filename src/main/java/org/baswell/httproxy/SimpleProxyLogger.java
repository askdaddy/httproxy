package org.baswell.httproxy;

import java.io.IOException;
import java.io.PrintStream;

/**
 * A ProxyLogger implementation that logs everything to System.out or System.err.
 */
public class SimpleProxyLogger implements ProxyLogger
{
  static public final int DEBUG_LEVEL = 0;

  static public final int INFO_LEVEL = 1;

  static public final int WARN_LEVEL = 2;

  static public final int ERROR_LEVEL = 3;
  
  private final int level;

  public SimpleProxyLogger(int level)
  {
    this.level = level;
  }

  @Override
  public boolean logDebugs()
  {
    return DEBUG_LEVEL >= level;
  }

  @Override
  public void debug(String message)
  {
    log(System.out, message, null, DEBUG_LEVEL);
  }

  @Override
  public void debug(String message, Throwable exception)
  {
    log(System.out, message, exception, DEBUG_LEVEL);
  }

  @Override
  public boolean logInfos()
  {
    return INFO_LEVEL >= level;
  }

  @Override
  public void info(String message)
  {
    log(System.out, message, null, INFO_LEVEL);
  }

  @Override
  public void info(String message, Throwable exception)
  {
    log(System.out, message, exception, INFO_LEVEL);
  }

  @Override
  public boolean logWarns()
  {
    return WARN_LEVEL >= level;
  }

  @Override
  public void warn(String message)
  {
    log(System.err, message, null, WARN_LEVEL);
  }

  @Override
  public void warn(String message, Throwable exception)
  {
    log(System.err, message, exception, WARN_LEVEL);
  }

  @Override
  public boolean logErrors()
  {
    return ERROR_LEVEL >= level;
  }

  @Override
  public void error(String message)
  {
    log(System.err, message, null, ERROR_LEVEL);
  }

  @Override
  public void error(String message, Throwable exception)
  {
    log(System.err, message, exception, ERROR_LEVEL);
  }

  void log(PrintStream ps, String message, Throwable exception, int atLevel)
  {
    if (atLevel >= level)
    {
      try
      {
        if (message != null)
        {
          ps.write((message + "\n").getBytes());
        }

        if (exception != null)
        {
          exception.printStackTrace(ps);
        }
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }
}

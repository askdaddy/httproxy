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

class WrappedLogger implements ProxyLogger
{
  private final ProxyLogger logger;

  WrappedLogger(ProxyLogger logger)
  {
    this.logger = logger;
  }

  @Override
  public boolean logDebugs()
  {
    return logger != null && logger.logDebugs();
  }

  @Override
  public void debug(String message)
  {
    if (logger != null)
    {
      logger.debug(message);
    }
  }

  @Override
  public boolean logInfos()
  {
    return logger != null &&logger.logInfos();
  }

  @Override
  public void info(String message)
  {
    if (logger != null)
    {
      logger.info(message);
    }
  }

  @Override
  public void warn(String message)
  {
    if (logger != null)
    {
      logger.warn(message);
    }
  }

  @Override
  public void warn(String message, Throwable exception)
  {
    if (logger != null)
    {
      logger.warn(message, exception);
    }
  }

  @Override
  public void error(String message)
  {
    if (logger != null)
    {
      logger.error(message);
    }
  }

  @Override
  public void error(String message, Throwable exception)
  {
    if (logger != null)
    {
      logger.error(message, exception);
    }
  }
}

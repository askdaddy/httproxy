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

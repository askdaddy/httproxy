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

/**
 * Bridge between your logging system and the HttProxy runtime.
 * 
 * @see ProxyDirector#getLogger() 
 */
public interface ProxyLogger
{
  /**
   * @return Is logging at level debug enabled?
   */
  boolean logDebugs();

  /**
   * Log if at level debug are higher.
   * 
   * @param message The log message.
   */
  void debug(String message);

  /**
   * Log if at level debug are higher.
   *
   * @param message The log message.
   * @param exception An exception to include with the given message.               
   */
  void debug(String message, Throwable exception);

  /**
   * @return Is logging at level info enabled?
   */
  boolean logInfos();

  /**
   * Log if at level info are higher.
   *
   * @param message The log message.
   */
  void info(String message);

  /**
   * Log if at level info are higher.
   *
   * @param message The log message.
   * @param exception An exception to include with the given message.
   */
  void info(String message, Throwable exception);

  /**
   * @return Is logging at level warn enabled?
   */
  boolean logWarns();

  /**
   * Log if at level warn higher.
   *
   * @param message The log message.
   */
  void warn(String message);

  /**
   * Log if at level warn are higher.
   *
   * @param message The log message.
   * @param exception An exception to include with the given message.
   */
  void warn(String message, Throwable exception);

  /**
   * @return Is logging at level error enabled?
   */
  boolean logErrors();

  /**
   * Log if at level error are higher.
   *
   * @param message The log message.
   */
  void error(String message);

  /**
   * Log if at level error are higher.
   *
   * @param message The log message.
   * @param exception An exception to include with the given message.
   */
  void error(String message, Throwable exception);

}

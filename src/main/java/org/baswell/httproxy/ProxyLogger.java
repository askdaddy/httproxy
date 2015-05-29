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

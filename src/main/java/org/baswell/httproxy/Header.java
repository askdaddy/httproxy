package org.baswell.httproxy;

/**
 * An HTTP header.
 */
public class Header
{
  public final String name;

  public final String value;

  public Header(String name, String value)
  {
    this.name = name;
    this.value = value;
  }
}

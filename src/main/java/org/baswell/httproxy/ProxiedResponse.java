package org.baswell.httproxy;

public interface ProxiedResponse
{
  long startedAt();

  long endedAt();

  int status();

  String reason();

  Object attachment();

  void attach(Object attachement);
}

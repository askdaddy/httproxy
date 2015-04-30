package org.baswell.httproxy;

public interface ProxiedRequest
{
  long startedAt();

  long endedAt();

  String host();

  String method();

  String path();

  Object attachment();

  void attach(Object attachement);
}

package org.baswell.httproxy;

enum ReadChunkedInputState
{
  READ_BYTE_COUNT,
  READ_BYTES,
  CLEAR_READ_BYTES_TERMINATOR,
  READ_LAST_LINE;
}

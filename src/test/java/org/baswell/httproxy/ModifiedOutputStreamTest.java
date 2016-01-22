package org.baswell.httproxy;

import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static org.baswell.httproxy.ModifiedOutputStream.*;

public class ModifiedOutputStreamTest
{
  @Test
  public void testContentTypeToCharset()
  {
    Charset utf8 = Charset.forName("UTF-8");
    Charset iso88591 = Charset.forName("ISO-8859-1");
    Charset usAscii = Charset.forName("US-ASCII");

    assertNotNull(utf8);
    assertNotNull(iso88591);
    assertNotNull(usAscii);

    assertEquals(utf8, contentTypeToCharset("application/json; charset=utf-8", null));
    assertNull(contentTypeToCharset("application/json", null));

    assertEquals(iso88591, contentTypeToCharset("text/html; charset=iso-8859-1", null));

    assertEquals(usAscii, contentTypeToCharset("text/html; charset=us-ascii;", null));
  }
}

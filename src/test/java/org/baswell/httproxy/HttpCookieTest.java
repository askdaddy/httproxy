package org.baswell.httproxy;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

public class HttpCookieTest
{

  @Test
  public void setSetCookie()
  {
    HttpCookie cookie = new HttpCookie("lu=Rg3vHJZnehYLjVg7qi3bZjzg; Expires=Tue, 15-Jan-2013 21:47:38 GMT; Path=/; Domain=.example.com; HttpOnly");

    assertEquals("lu", cookie.name);
    assertEquals("Rg3vHJZnehYLjVg7qi3bZjzg", cookie.value);
    assertEquals("/", cookie.path);
    assertEquals(".example.com", cookie.domain);
    assertTrue(cookie.httpOnly);
    assertFalse(cookie.secure);

    assertNotNull(cookie.expiresAt);

    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(cookie.expiresAt);

    assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
    assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH));
    assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH));
    assertEquals(2013, calendar.get(Calendar.YEAR));
    assertEquals(21, calendar.get(Calendar.HOUR_OF_DAY) - (calendar.getTimeZone().getRawOffset() / (1000 * 60 * 60)));
    assertEquals(47, calendar.get(Calendar.MINUTE));
    assertEquals(38, calendar.get(Calendar.SECOND));

    HttpCookie encodedCookie = new HttpCookie(cookie.toString());
    assertEquals(cookie.name, encodedCookie.name);
    assertEquals(cookie.value, encodedCookie.value);
    assertEquals(cookie.path, cookie.path);
    assertEquals(cookie.domain, encodedCookie.domain);
    assertEquals(cookie.httpOnly, encodedCookie.httpOnly);
    assertEquals(cookie.secure, encodedCookie.secure);
    assertEquals(cookie.expiresAt, encodedCookie.expiresAt);
  }

  @Test
  public void setCookies()
  {

  }
}

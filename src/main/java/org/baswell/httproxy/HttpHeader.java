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

import gnu.trove.list.array.TByteArrayList;
import static org.baswell.httproxy.Constants.*;

/**
 * An HTTP header.
 */
public class HttpHeader
{
  public String name;

  public String value;

  public HttpHeader(String name, String value)
  {
    this.name = name;
    this.value = value;
  }

  void addTo(TByteArrayList bytes)
  {
    bytes.add(name.getBytes());
    bytes.add(": ".getBytes());
    bytes.add(value.getBytes());
    bytes.add(CR);
    bytes.add(LF);
  }
}
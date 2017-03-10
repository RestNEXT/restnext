/*
 * Copyright (C) 2016 Thiago Gutenberg Carvalho da Costa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.restnext.core.http;

import io.netty.util.AsciiString;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by thiago on 06/09/16.
 */
interface Headers {

  default String getHeader(AsciiString name) {
    return getHeader(name.toString());
  }

  default String getHeader(String name) {
    List<String> values = getAllHeader(name);
    if (values == null) {
      return null;
    }
    if (values.isEmpty()) {
      return "";
    }
    return values.stream().collect(Collectors.joining(","));
  }

  default List<String> getAllHeader(AsciiString name) {
    return getAllHeader(name.toString());
  }

  default List<String> getAllHeader(String name) {
    return getHeaders().get(name);
  }

  MultivaluedMap<String, String> getHeaders();

}

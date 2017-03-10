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

package org.restnext.security.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;

import org.junit.Test;
import org.restnext.core.jaxb.Jaxb;

/**
 * Created by thiago on 10/03/17.
 */
public class SecuritiesTest {

  private final String securityXsd = "security.xsd";
  private final String securityXml = "security.xml";
  private final String securityInvalidXml = "securityInvalid.xml";
  private final String securityFormattedXml = "securityFormatted.xml";
  private final String securityFormattedInvalidXml = "securityFormattedInvalid.xml";


  private final Jaxb securityJaxb = new Jaxb(Securities.class);
  private final Jaxb securityJaxbWithSchemaValidation = new Jaxb(securityXsd, Securities.class);

  @Test
  public void securityFiles() throws IOException {
    checkFile(securityXsd, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIj8+Cjx4czpzY2hlbWEgeG1sbnM6eHM9Imh0dHA6Ly93d3cudzMub3Jn"
        + "LzIwMDEvWE1MU2NoZW1hIgogICAgICAgICAgIHRhcmdldE5hbWVzcGFjZT0iaHR0cDovL3d3dy5yZXN0"
        + "bmV4dC5vcmcvc2VjdXJpdGllcyIKICAgICAgICAgICB4bWxucz0iaHR0cDovL3d3dy5yZXN0bmV4dC5v"
        + "cmcvc2VjdXJpdGllcyIKICAgICAgICAgICBlbGVtZW50Rm9ybURlZmF1bHQ9InF1YWxpZmllZCI+Cgog"
        + "IDx4czplbGVtZW50IG5hbWU9InNlY3VyaXRpZXMiPgogICAgPHhzOmFubm90YXRpb24+CiAgICAgIDx4"
        + "czphcHBpbmZvPlRoaXMgc2NoZW1hIGRlZmluZXMgYSBSZXN0TkVYVCBTZWN1cml0eSBNZXRhZGF0YS48"
        + "L3hzOmFwcGluZm8+CiAgICAgIDx4czpkb2N1bWVudGF0aW9uIHNvdXJjZT0iZGVzY3JpcHRpb24iPgog"
        + "ICAgICAgIFRoaXMgaXMgdGhlIHJvb3QgZWxlbWVudCBvZiB0aGUgZGVzY3JpcHRvci4KICAgICAgPC94"
        + "czpkb2N1bWVudGF0aW9uPgogICAgPC94czphbm5vdGF0aW9uPgogICAgPHhzOmNvbXBsZXhUeXBlPgog"
        + "ICAgICA8eHM6c2VxdWVuY2U+CiAgICAgICAgPHhzOmVsZW1lbnQgbmFtZT0ic2VjdXJpdHkiIG1heE9j"
        + "Y3Vycz0idW5ib3VuZGVkIj4KICAgICAgICAgIDx4czphbm5vdGF0aW9uPgogICAgICAgICAgICA8eHM6"
        + "ZG9jdW1lbnRhdGlvbiBzb3VyY2U9ImRlc2NyaXB0aW9uIj4KICAgICAgICAgICAgICBUaGlzIGVsZW1l"
        + "bnQgcmVwcmVzZW50cyB0aGUgc2VjdXJpdHkgbWV0YWRhdGEuCiAgICAgICAgICAgIDwveHM6ZG9jdW1l"
        + "bnRhdGlvbj4KICAgICAgICAgIDwveHM6YW5ub3RhdGlvbj4KICAgICAgICAgIDx4czpjb21wbGV4VHlw"
        + "ZT4KICAgICAgICAgICAgPHhzOmFsbD4KICAgICAgICAgICAgICA8IS0tIGNvbW1lbnQgdGhpcyBwYXRo"
        + "IGVsZW1lbnQgd2l0aCByZWdleCB2YWxpZGF0aW9uCiAgICAgICAgICAgICAgYmVjYXVzZSB0aGlzIGVu"
        + "dHJ5IGNhbiBiZToKICAgICAgICAgICAgICBhIHBhdGggKC90ZXN0KSwKICAgICAgICAgICAgICBhIHBh"
        + "dGggcGFyYW0gKC90ZXN0L3tuYW1lfSkgb3IKICAgICAgICAgICAgICBhIHBhdGggcmVnZXggKC90ZXN0"
        + "L3JlZ2V4L1xcZCspLgogICAgICAgICAgICAgIEFuZCB0aGlzIHJlZ2V4IG9ubHkgbWF0Y2ggYXMgdmFs"
        + "aWQgdGhlIGVudHJpZXM6IChwYXRoIGFuZCBwYXRoIHBhcmFtKS4KICAgICAgICAgICAgICA8eHM6ZWxl"
        + "bWVudCBuYW1lPSJwYXRoIj4KICAgICAgICAgICAgICAgICAgPHhzOmFubm90YXRpb24+CiAgICAgICAg"
        + "ICAgICAgICAgICAgICA8eHM6ZG9jdW1lbnRhdGlvbiBzb3VyY2U9ImRlc2NyaXB0aW9uIj4KICAgICAg"
        + "ICAgICAgICAgICAgICAgICAgICBUaGlzIGVsZW1lbnQgZGVmaW5lcyB0aGUgc2VjdXJpdHkgcGF0aC4K"
        + "ICAgICAgICAgICAgICAgICAgICAgIDwveHM6ZG9jdW1lbnRhdGlvbj4KICAgICAgICAgICAgICAgICAg"
        + "PC94czphbm5vdGF0aW9uPgogICAgICAgICAgICAgICAgICA8eHM6c2ltcGxlVHlwZT4KICAgICAgICAg"
        + "ICAgICAgICAgICAgIDx4czpyZXN0cmljdGlvbiBiYXNlPSJ4czpzdHJpbmciPgogICAgICAgICAgICAg"
        + "ICAgICAgICAgICAgIDx4czpwYXR0ZXJuIHZhbHVlPSIoWy9dKSgoWy9cd10pKygvXHtbXHddK1x9KSop"
        + "KihbP10pPyIvPgogICAgICAgICAgICAgICAgICAgICAgPC94czpyZXN0cmljdGlvbj4KICAgICAgICAg"
        + "ICAgICAgICAgPC94czpzaW1wbGVUeXBlPgogICAgICAgICAgICAgIDwveHM6ZWxlbWVudD4KICAgICAg"
        + "ICAgICAgICAtLT4KICAgICAgICAgICAgICA8eHM6ZWxlbWVudCBuYW1lPSJwYXRoIiB0eXBlPSJ4czpz"
        + "dHJpbmciPgogICAgICAgICAgICAgICAgPHhzOmFubm90YXRpb24+CiAgICAgICAgICAgICAgICAgIDx4"
        + "czpkb2N1bWVudGF0aW9uIHNvdXJjZT0iZGVzY3JpcHRpb24iPgogICAgICAgICAgICAgICAgICAgIFRo"
        + "aXMgZWxlbWVudCBkZWZpbmVzIHRoZSBzZWN1cml0eSBwYXRoLgogICAgICAgICAgICAgICAgICA8L3hz"
        + "OmRvY3VtZW50YXRpb24+CiAgICAgICAgICAgICAgICA8L3hzOmFubm90YXRpb24+CiAgICAgICAgICAg"
        + "ICAgPC94czplbGVtZW50PgoKICAgICAgICAgICAgICA8eHM6ZWxlbWVudCBuYW1lPSJwcm92aWRlciI+"
        + "CiAgICAgICAgICAgICAgICA8eHM6YW5ub3RhdGlvbj4KICAgICAgICAgICAgICAgICAgPHhzOmRvY3Vt"
        + "ZW50YXRpb24gc291cmNlPSJkZXNjcmlwdGlvbiI+CiAgICAgICAgICAgICAgICAgICAgVGhpcyBlbGVt"
        + "ZW50IGRlZmluZXMgdGhlIGxhbWJkYSBzdHJpbmcgbWV0aG9kIHJlZmVyZW5jZSBzZWN1cml0eSBwcm92"
        + "aWRlci4KICAgICAgICAgICAgICAgICAgPC94czpkb2N1bWVudGF0aW9uPgogICAgICAgICAgICAgICAg"
        + "PC94czphbm5vdGF0aW9uPgogICAgICAgICAgICAgICAgPHhzOnNpbXBsZVR5cGU+CiAgICAgICAgICAg"
        + "ICAgICAgIDx4czpyZXN0cmljdGlvbiBiYXNlPSJ4czpzdHJpbmciPgogICAgICAgICAgICAgICAgICAg"
        + "IDx4czpwYXR0ZXJuIHZhbHVlPSIoW1x3Ll0pKihbOl17Mn0pKFx3KSsiLz4KICAgICAgICAgICAgICAg"
        + "ICAgPC94czpyZXN0cmljdGlvbj4KICAgICAgICAgICAgICAgIDwveHM6c2ltcGxlVHlwZT4KICAgICAg"
        + "ICAgICAgICA8L3hzOmVsZW1lbnQ+CgogICAgICAgICAgICAgIDx4czplbGVtZW50IG5hbWU9ImVuYWJs"
        + "ZSIgdHlwZT0ieHM6Ym9vbGVhbiIgbWluT2NjdXJzPSIwIiBkZWZhdWx0PSJ0cnVlIj4KICAgICAgICAg"
        + "ICAgICAgIDx4czphbm5vdGF0aW9uPgogICAgICAgICAgICAgICAgICA8eHM6ZG9jdW1lbnRhdGlvbiBz"
        + "b3VyY2U9ImRlc2NyaXB0aW9uIj4KICAgICAgICAgICAgICAgICAgICBUaGlzIGVsZW1lbnQgZGVmaW5l"
        + "cyBpZiB0aGlzIHNlY3VyaXR5IHBhdGggaXMgZW5hYmxlIG9yIG5vdC4KICAgICAgICAgICAgICAgICAg"
        + "PC94czpkb2N1bWVudGF0aW9uPgogICAgICAgICAgICAgICAgPC94czphbm5vdGF0aW9uPgogICAgICAg"
        + "ICAgICAgIDwveHM6ZWxlbWVudD4KCiAgICAgICAgICAgIDwveHM6YWxsPgogICAgICAgICAgPC94czpj"
        + "b21wbGV4VHlwZT4KICAgICAgICA8L3hzOmVsZW1lbnQ+CiAgICAgIDwveHM6c2VxdWVuY2U+CiAgICA8"
        + "L3hzOmNvbXBsZXhUeXBlPgogIDwveHM6ZWxlbWVudD4KCjwveHM6c2NoZW1hPg==");
    checkFile(securityXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/PjxzZWN1"
        + "cml0aWVzIHhtbG5zPSJodHRwOi8vd3d3LnJlc3RuZXh0Lm9yZy9zZWN1cml0aWVzIj48c2VjdXJpdHk+"
        + "PHBhdGg+L3Rlc3Q8L3BhdGg+PHByb3ZpZGVyPmJyLmNvbS50aGlhZ3V0ZW4uc2VjdXJpdHkuUHJvdmlk"
        + "ZXI6OmFueU1ldGhvZE5hbWVZb3VXYW50PC9wcm92aWRlcj48L3NlY3VyaXR5PjxzZWN1cml0eT48cGF0"
        + "aD4vdGVzdC97bmFtZX08L3BhdGg+PHByb3ZpZGVyPmJyLmNvbS50aGlhZ3V0ZW4uc2VjdXJpdHkuUHJv"
        + "dmlkZXI6OnRlc3QyPC9wcm92aWRlcj48ZW5hYmxlPmZhbHNlPC9lbmFibGU+PC9zZWN1cml0eT48c2Vj"
        + "dXJpdHk+PHBhdGg+L3Rlc3QvcmVnZXgvXFxkKzwvcGF0aD48cHJvdmlkZXI+YnIuY29tLnRoaWFndXRl"
        + "bi5zZWN1cml0eS5Qcm92aWRlcjo6dGVzdDM8L3Byb3ZpZGVyPjwvc2VjdXJpdHk+PC9zZWN1cml0aWVz"
        + "Pg==");
    checkFile(securityInvalidXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/PjxzZWN1"
        + "cml0aWVzIHhtbG5zPSJodHRwOi8vd3d3LnJlc3RuZXh0Lm9yZy9zZWN1cml0aWVzIj48c2VjdXJpdHk+"
        + "PHBhdGg+bGFsYWxhbGFsYTwvcGF0aD48cHJvdmlkZXI+YnIuY29tLnRoaWFndXRlbi5zZWN1cml0eS5Q"
        + "cm92aWRlcjo6Ojo6Ojo6OiQkJDwvcHJvdmlkZXI+PC9zZWN1cml0eT48c2VjdXJpdHk+PHBhdGg+L3Rl"
        + "c3Qve25hbWV9PC9wYXRoPjxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnNlY3VyaXR5LlByb3ZpZGVy"
        + "Ojp0ZXN0MjwvcHJvdmlkZXI+PGVuYWJsZT5mYWxzZTwvZW5hYmxlPjwvc2VjdXJpdHk+PHNlY3VyaXR5"
        + "PjxwYXRoPi90ZXN0L3JlZ2V4L1xcZCs8L3BhdGg+PHByb3ZpZGVyPmJyLmNvbS50aGlhZ3V0ZW4uc2Vj"
        + "dXJpdHkuUHJvdmlkZXI6OnRlc3QzPC9wcm92aWRlcj48L3NlY3VyaXR5Pjwvc2VjdXJpdGllcz4=");
    checkFile(securityFormattedXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pgo8c2Vj"
        + "dXJpdGllcyB4bWxucz0iaHR0cDovL3d3dy5yZXN0bmV4dC5vcmcvc2VjdXJpdGllcyI+CiAgICA8c2Vj"
        + "dXJpdHk+CiAgICAgICAgPHBhdGg+L3Rlc3Q8L3BhdGg+CiAgICAgICAgPHByb3ZpZGVyPmJyLmNvbS50"
        + "aGlhZ3V0ZW4uc2VjdXJpdHkuUHJvdmlkZXI6OmFueU1ldGhvZE5hbWVZb3VXYW50PC9wcm92aWRlcj4K"
        + "ICAgIDwvc2VjdXJpdHk+CiAgICA8c2VjdXJpdHk+CiAgICAgICAgPHBhdGg+L3Rlc3Qve25hbWV9PC9w"
        + "YXRoPgogICAgICAgIDxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnNlY3VyaXR5LlByb3ZpZGVyOjp0"
        + "ZXN0MjwvcHJvdmlkZXI+CiAgICAgICAgPGVuYWJsZT5mYWxzZTwvZW5hYmxlPgogICAgPC9zZWN1cml0"
        + "eT4KICAgIDxzZWN1cml0eT4KICAgICAgICA8cGF0aD4vdGVzdC9yZWdleC9cXGQrPC9wYXRoPgogICAg"
        + "ICAgIDxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnNlY3VyaXR5LlByb3ZpZGVyOjp0ZXN0MzwvcHJv"
        + "dmlkZXI+CiAgICA8L3NlY3VyaXR5Pgo8L3NlY3VyaXRpZXM+Cgo=");
    checkFile(securityFormattedInvalidXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pgo8c2Vj"
        + "dXJpdGllcyB4bWxucz0iaHR0cDovL3d3dy5yZXN0bmV4dC5vcmcvc2VjdXJpdGllcyI+CiAgICA8c2Vj"
        + "dXJpdHk+CiAgICAgICAgPHBhdGg+YWxhbGFsYWxhbGFsYTwvcGF0aD4KICAgICAgICA8cHJvdmlkZXI+"
        + "JSUlJCMjPC9wcm92aWRlcj4KICAgIDwvc2VjdXJpdHk+CiAgICA8c2VjdXJpdHk+CiAgICAgICAgPHBh"
        + "dGg+L3Rlc3Qve25hbWV9PC9wYXRoPgogICAgICAgIDxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnNl"
        + "Y3VyaXR5LlByb3ZpZGVyOjo6Ojo6Ojo6Ojp0ZXN0MjwvcHJvdmlkZXI+CiAgICAgICAgPGVuYWJsZT5m"
        + "YWxzZTwvZW5hYmxlPgogICAgPC9zZWN1cml0eT4KICAgIDxzZWN1cml0eT4KICAgICAgICA8cGF0aD4v"
        + "dGVzdC9yZWdleC9cXGQrPC9wYXRoPgogICAgICAgIDxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnNl"
        + "Y3VyaXR5LlByb3ZpZGVyOjp0ZXN0MzwvcHJvdmlkZXI+CiAgICA8L3NlY3VyaXR5Pgo8L3NlY3VyaXRp"
        + "ZXM+Cgo=");
  }

  private void checkFile(String name, String expectedEncoded) throws IOException {
    URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
    assertNotNull(resource);
    URI uri = URI.create(resource.toString());
    assertEquals("file", uri.getScheme());
    Path path = Paths.get(uri);
    assertEquals(name, path.getFileName().toString());
    byte[] bytes = Files.readAllBytes(path);
    String encode = Base64.getEncoder().encodeToString(bytes);
    assertEquals(expectedEncoded, encode);
  }

  @Test
  public void securityMarshalAndUnmarshalTest() throws JAXBException, IOException {
    securityMarshalAndUnmarshal(securityXml, securityJaxb, false);
    securityMarshalAndUnmarshal(securityInvalidXml, securityJaxb, false);
    securityMarshalAndUnmarshal(securityFormattedXml, securityJaxb, true);
    securityMarshalAndUnmarshal(securityFormattedInvalidXml, securityJaxb, true);
    securityMarshalAndUnmarshal(securityXml, securityJaxbWithSchemaValidation, false);
    securityMarshalAndUnmarshal(
        securityFormattedXml, securityJaxbWithSchemaValidation, true);
  }

  private void securityMarshalAndUnmarshal(String xml, Jaxb jaxb, boolean formatted)
      throws JAXBException, IOException {
    final Class<Securities> returnClass = Securities.class;
    URL resource = Thread.currentThread().getContextClassLoader().getResource(xml);
    assertNotNull(resource);
    URI uri = URI.create(resource.toString());
    assertEquals("file", uri.getScheme());
    Path path = Paths.get(uri);
    assertEquals(xml, path.getFileName().toString());

    // unmarshal with path input
    Securities deserialized = jaxb.unmarshal(path, returnClass);
    assertNotNull(deserialized);
    List<Securities.Security> security = deserialized.getSecurity();
    assertEquals(3, security.size());

    // unmarshal with input stream input
    try (InputStream inputStream = Files.newInputStream(path)) {
      deserialized = jaxb.unmarshal(inputStream, returnClass);
      assertNotNull(deserialized);
      security = deserialized.getSecurity();
      assertEquals(3, security.size());
    }

    // unmarshal with string input
    Collector<CharSequence, ?, String> collector = formatted
        ? Collectors.joining("\n")
        : Collectors.joining();
    String string = Files.lines(path).collect(collector);
    deserialized = jaxb.unmarshal(string, returnClass);
    assertNotNull(deserialized);
    security = deserialized.getSecurity();
    assertEquals(3, security.size());

    // marshal
    String serialized = jaxb.marshal(deserialized,
        Collections.singletonMap(Marshaller.JAXB_FORMATTED_OUTPUT, formatted));
    assertEquals(string, serialized);
  }

  @Test(expected = UnmarshalException.class)
  public void securityInvalidMarshalAndUnmarshalTest() throws JAXBException, IOException {
    securityMarshalAndUnmarshal(securityInvalidXml,
        securityJaxbWithSchemaValidation, false);
    securityMarshalAndUnmarshal(securityFormattedInvalidXml,
        securityJaxbWithSchemaValidation, true);
  }
}

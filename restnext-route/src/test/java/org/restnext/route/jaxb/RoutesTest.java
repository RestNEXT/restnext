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

package org.restnext.route.jaxb;

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
public class RoutesTest {

  private final String routesXsd = "routes.xsd";

  private final String routesXml = "routes.xml";
  private final String routesInvalidXml = "routesInvalid.xml";
  private final String routesFormattedXml = "routesFormatted.xml";
  private final String routesFormattedInvalidXml = "routesFormattedInvalid.xml";

  private final Jaxb routeJaxb = new Jaxb(Routes.class);
  private final Jaxb routeJaxbWithSchemaValidation = new Jaxb(routesXsd, Routes.class);

  @Test
  public void routesMarshalAndUnmarshalTest() throws JAXBException, IOException {
    routesMarshalAndUnmarshal(routesXml, routeJaxb, false);
    routesMarshalAndUnmarshal(routesInvalidXml, routeJaxb, false);
    routesMarshalAndUnmarshal(routesFormattedXml, routeJaxb, true);
    routesMarshalAndUnmarshal(routesFormattedInvalidXml, routeJaxb, true);
    routesMarshalAndUnmarshal(routesXml, routeJaxbWithSchemaValidation, false);
    routesMarshalAndUnmarshal(routesFormattedXml, routeJaxbWithSchemaValidation, true);
  }

  private void routesMarshalAndUnmarshal(String xml, Jaxb jaxb, boolean formatted)
      throws JAXBException, IOException {
    final Class<Routes> returnClass = Routes.class;
    URL resource = Thread.currentThread().getContextClassLoader().getResource(xml);
    assertNotNull(resource);
    URI uri = URI.create(resource.toString());
    assertEquals("file", uri.getScheme());
    Path path = Paths.get(uri);
    assertEquals(xml, path.getFileName().toString());

    // unmarshal with path input
    Routes deserialized = jaxb.unmarshal(path, returnClass);
    assertNotNull(deserialized);
    List<Routes.Route> route = deserialized.getRoute();
    assertEquals(3, route.size());

    // unmarshal with input stream input
    try (InputStream inputStream = Files.newInputStream(path)) {
      deserialized = jaxb.unmarshal(inputStream, returnClass);
      assertNotNull(deserialized);
      route = deserialized.getRoute();
      assertEquals(3, route.size());
    }

    // unmarshal with string input
    Collector<CharSequence, ?, String> collector = formatted
        ? Collectors.joining("\n")
        : Collectors.joining();
    String string = Files.lines(path).collect(collector);
    deserialized = jaxb.unmarshal(string, returnClass);
    assertNotNull(deserialized);
    route = deserialized.getRoute();
    assertEquals(3, route.size());

    // marshal
    String serialized = jaxb.marshal(deserialized,
        Collections.singletonMap(Marshaller.JAXB_FORMATTED_OUTPUT, formatted));
    assertEquals(string, serialized);
  }

  @Test(expected = UnmarshalException.class)
  public void routesInvalidMarshalAndUnmarshalTest() throws JAXBException, IOException {
    routesMarshalAndUnmarshal(
        routesInvalidXml, routeJaxbWithSchemaValidation, false);
    routesMarshalAndUnmarshal(
        routesFormattedInvalidXml, routeJaxbWithSchemaValidation, true);
  }

  @Test
  public void routeFiles() throws IOException {
    checkFile(routesXsd, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIj8+Cjx4czpzY2hlbWEgeG1sbnM6eHM9Imh0dHA6Ly93d3cudzMub3Jn"
        + "LzIwMDEvWE1MU2NoZW1hIgogICAgICAgICAgIHRhcmdldE5hbWVzcGFjZT0iaHR0cDovL3d3dy5yZXN0"
        + "bmV4dC5vcmcvcm91dGVzIgogICAgICAgICAgIHhtbG5zPSJodHRwOi8vd3d3LnJlc3RuZXh0Lm9yZy9y"
        + "b3V0ZXMiCiAgICAgICAgICAgZWxlbWVudEZvcm1EZWZhdWx0PSJxdWFsaWZpZWQiPgoKICA8eHM6ZWxl"
        + "bWVudCBuYW1lPSJyb3V0ZXMiPgogICAgPHhzOmFubm90YXRpb24+CiAgICAgIDx4czphcHBpbmZvPlRo"
        + "aXMgc2NoZW1hIGRlZmluZXMgYSBSZXN0TkVYVCBSb3V0ZSBNZXRhZGF0YS48L3hzOmFwcGluZm8+CiAg"
        + "ICAgIDx4czpkb2N1bWVudGF0aW9uIHNvdXJjZT0iZGVzY3JpcHRpb24iPgogICAgICAgIFRoaXMgaXMg"
        + "dGhlIHJvb3QgZWxlbWVudCBvZiB0aGUgZGVzY3JpcHRvci4KICAgICAgPC94czpkb2N1bWVudGF0aW9u"
        + "PgogICAgPC94czphbm5vdGF0aW9uPgogICAgPHhzOmNvbXBsZXhUeXBlPgogICAgICA8eHM6c2VxdWVu"
        + "Y2U+CiAgICAgICAgPHhzOmVsZW1lbnQgbmFtZT0icm91dGUiIG1heE9jY3Vycz0idW5ib3VuZGVkIj4K"
        + "ICAgICAgICAgIDx4czphbm5vdGF0aW9uPgogICAgICAgICAgICA8eHM6ZG9jdW1lbnRhdGlvbiBzb3Vy"
        + "Y2U9ImRlc2NyaXB0aW9uIj4KICAgICAgICAgICAgICBUaGlzIGVsZW1lbnQgcmVwcmVzZW50cyB0aGUg"
        + "cm91dGUgbWV0YWRhdGEuCiAgICAgICAgICAgIDwveHM6ZG9jdW1lbnRhdGlvbj4KICAgICAgICAgIDwv"
        + "eHM6YW5ub3RhdGlvbj4KICAgICAgICAgIDx4czpjb21wbGV4VHlwZT4KICAgICAgICAgICAgPHhzOmFs"
        + "bD4KICAgICAgICAgICAgICA8IS0tIGNvbW1lbnQgdGhpcyBwYXRoIGVsZW1lbnQgd2l0aCByZWdleCB2"
        + "YWxpZGF0aW9uCiAgICAgICAgICAgICAgYmVjYXVzZSB0aGlzIGVudHJ5IGNhbiBiZToKICAgICAgICAg"
        + "ICAgICBhIHBhdGggKC90ZXN0KSwKICAgICAgICAgICAgICBhIHBhdGggcGFyYW0gKC90ZXN0L3tuYW1l"
        + "fSkgb3IKICAgICAgICAgICAgICBhIHBhdGggcmVnZXggKC90ZXN0L3JlZ2V4L1xcZCspLgogICAgICAg"
        + "ICAgICAgIEFuZCB0aGlzIHJlZ2V4IG9ubHkgbWF0Y2ggYXMgdmFsaWQgdGhlIGVudHJpZXM6IChwYXRo"
        + "IGFuZCBwYXRoIHBhcmFtKS4KICAgICAgICAgICAgICA8eHM6ZWxlbWVudCBuYW1lPSJwYXRoIj4KICAg"
        + "ICAgICAgICAgICAgICAgPHhzOmFubm90YXRpb24+CiAgICAgICAgICAgICAgICAgICAgICA8eHM6ZG9j"
        + "dW1lbnRhdGlvbiBzb3VyY2U9ImRlc2NyaXB0aW9uIj4KICAgICAgICAgICAgICAgICAgICAgICAgICBU"
        + "aGlzIGVsZW1lbnQgZGVmaW5lcyB0aGUgcm91dGUgcGF0aC4KICAgICAgICAgICAgICAgICAgICAgIDwv"
        + "eHM6ZG9jdW1lbnRhdGlvbj4KICAgICAgICAgICAgICAgICAgPC94czphbm5vdGF0aW9uPgogICAgICAg"
        + "ICAgICAgICAgICA8eHM6c2ltcGxlVHlwZT4KICAgICAgICAgICAgICAgICAgICAgIDx4czpyZXN0cmlj"
        + "dGlvbiBiYXNlPSJ4czpzdHJpbmciPgogICAgICAgICAgICAgICAgICAgICAgICAgIDx4czpwYXR0ZXJu"
        + "IHZhbHVlPSIoWy9dKSgoWy9cd10pKygvXHtbXHddK1x9KSopKihbP10pPyIvPgogICAgICAgICAgICAg"
        + "ICAgICAgICAgPC94czpyZXN0cmljdGlvbj4KICAgICAgICAgICAgICAgICAgPC94czpzaW1wbGVUeXBl"
        + "PgogICAgICAgICAgICAgIDwveHM6ZWxlbWVudD4KICAgICAgICAgICAgICAtLT4KICAgICAgICAgICAg"
        + "ICA8eHM6ZWxlbWVudCBuYW1lPSJwYXRoIiB0eXBlPSJ4czpzdHJpbmciPgogICAgICAgICAgICAgICAg"
        + "PHhzOmFubm90YXRpb24+CiAgICAgICAgICAgICAgICAgIDx4czpkb2N1bWVudGF0aW9uIHNvdXJjZT0i"
        + "ZGVzY3JpcHRpb24iPgogICAgICAgICAgICAgICAgICAgIFRoaXMgZWxlbWVudCBkZWZpbmVzIHRoZSBy"
        + "b3V0ZSBwYXRoLgogICAgICAgICAgICAgICAgICA8L3hzOmRvY3VtZW50YXRpb24+CiAgICAgICAgICAg"
        + "ICAgICA8L3hzOmFubm90YXRpb24+CiAgICAgICAgICAgICAgPC94czplbGVtZW50PgoKICAgICAgICAg"
        + "ICAgICA8eHM6ZWxlbWVudCBuYW1lPSJwcm92aWRlciI+CiAgICAgICAgICAgICAgICA8eHM6YW5ub3Rh"
        + "dGlvbj4KICAgICAgICAgICAgICAgICAgPHhzOmRvY3VtZW50YXRpb24gc291cmNlPSJkZXNjcmlwdGlv"
        + "biI+CiAgICAgICAgICAgICAgICAgICAgVGhpcyBlbGVtZW50IGRlZmluZXMgdGhlIGxhbWJkYSBzdHJp"
        + "bmcgbWV0aG9kIHJlZmVyZW5jZSByb3V0ZSBwcm92aWRlci4KICAgICAgICAgICAgICAgICAgPC94czpk"
        + "b2N1bWVudGF0aW9uPgogICAgICAgICAgICAgICAgPC94czphbm5vdGF0aW9uPgogICAgICAgICAgICAg"
        + "ICAgPHhzOnNpbXBsZVR5cGU+CiAgICAgICAgICAgICAgICAgIDx4czpyZXN0cmljdGlvbiBiYXNlPSJ4"
        + "czpzdHJpbmciPgogICAgICAgICAgICAgICAgICAgIDx4czpwYXR0ZXJuIHZhbHVlPSIoW1x3Ll0pKihb"
        + "Ol17Mn0pKFx3KSsiLz4KICAgICAgICAgICAgICAgICAgPC94czpyZXN0cmljdGlvbj4KICAgICAgICAg"
        + "ICAgICAgIDwveHM6c2ltcGxlVHlwZT4KICAgICAgICAgICAgICA8L3hzOmVsZW1lbnQ+CgogICAgICAg"
        + "ICAgICAgIDx4czplbGVtZW50IG5hbWU9ImVuYWJsZSIgdHlwZT0ieHM6Ym9vbGVhbiIgbWluT2NjdXJz"
        + "PSIwIiBkZWZhdWx0PSJ0cnVlIj4KICAgICAgICAgICAgICAgIDx4czphbm5vdGF0aW9uPgogICAgICAg"
        + "ICAgICAgICAgICA8eHM6ZG9jdW1lbnRhdGlvbiBzb3VyY2U9ImRlc2NyaXB0aW9uIj4KICAgICAgICAg"
        + "ICAgICAgICAgICBUaGlzIGVsZW1lbnQgZGVmaW5lcyBpZiB0aGlzIHJvdXRlIHBhdGggaXMgZW5hYmxl"
        + "IG9yIG5vdC4KICAgICAgICAgICAgICAgICAgPC94czpkb2N1bWVudGF0aW9uPgogICAgICAgICAgICAg"
        + "ICAgPC94czphbm5vdGF0aW9uPgogICAgICAgICAgICAgIDwveHM6ZWxlbWVudD4KCiAgICAgICAgICAg"
        + "ICAgPHhzOmVsZW1lbnQgbmFtZT0ibWV0aG9kcyIgbWluT2NjdXJzPSIwIj4KICAgICAgICAgICAgICAg"
        + "IDx4czphbm5vdGF0aW9uPgogICAgICAgICAgICAgICAgICA8eHM6ZG9jdW1lbnRhdGlvbiBzb3VyY2U9"
        + "ImRlc2NyaXB0aW9uIj4KICAgICAgICAgICAgICAgICAgICBUaGlzIGVsZW1lbnQgZGVmaW5lcyB0aGUg"
        + "cm91dGUgYWxsb3dlZCBodHRwIG1ldGhvZHMuCiAgICAgICAgICAgICAgICAgIDwveHM6ZG9jdW1lbnRh"
        + "dGlvbj4KICAgICAgICAgICAgICAgIDwveHM6YW5ub3RhdGlvbj4KICAgICAgICAgICAgICAgIDx4czpj"
        + "b21wbGV4VHlwZT4KICAgICAgICAgICAgICAgICAgPHhzOnNlcXVlbmNlPgogICAgICAgICAgICAgICAg"
        + "ICAgIDx4czplbGVtZW50IG5hbWU9Im1ldGhvZCIgbWF4T2NjdXJzPSJ1bmJvdW5kZWQiPgogICAgICAg"
        + "ICAgICAgICAgICAgICAgPHhzOmFubm90YXRpb24+CiAgICAgICAgICAgICAgICAgICAgICAgIDx4czpk"
        + "b2N1bWVudGF0aW9uIHNvdXJjZT0iZGVzY3JpcHRpb24iPgogICAgICAgICAgICAgICAgICAgICAgICAg"
        + "IFRoaXMgZWxlbWVudCBkZWZpbmVzIGEgaHR0cCBtZXRob2QuCiAgICAgICAgICAgICAgICAgICAgICAg"
        + "IDwveHM6ZG9jdW1lbnRhdGlvbj4KICAgICAgICAgICAgICAgICAgICAgIDwveHM6YW5ub3RhdGlvbj4K"
        + "ICAgICAgICAgICAgICAgICAgICAgIDx4czpzaW1wbGVUeXBlPgogICAgICAgICAgICAgICAgICAgICAg"
        + "ICA8eHM6cmVzdHJpY3Rpb24gYmFzZT0ieHM6c3RyaW5nIj4KICAgICAgICAgICAgICAgICAgICAgICAg"
        + "ICA8eHM6ZW51bWVyYXRpb24gdmFsdWU9IkdFVCIvPgogICAgICAgICAgICAgICAgICAgICAgICAgIDx4"
        + "czplbnVtZXJhdGlvbiB2YWx1ZT0iUE9TVCIvPgogICAgICAgICAgICAgICAgICAgICAgICAgIDx4czpl"
        + "bnVtZXJhdGlvbiB2YWx1ZT0iUFVUIi8+CiAgICAgICAgICAgICAgICAgICAgICAgICAgPHhzOmVudW1l"
        + "cmF0aW9uIHZhbHVlPSJQQVRDSCIvPgogICAgICAgICAgICAgICAgICAgICAgICAgIDx4czplbnVtZXJh"
        + "dGlvbiB2YWx1ZT0iREVMRVRFIi8+CiAgICAgICAgICAgICAgICAgICAgICAgIDwveHM6cmVzdHJpY3Rp"
        + "b24+CiAgICAgICAgICAgICAgICAgICAgICA8L3hzOnNpbXBsZVR5cGU+CiAgICAgICAgICAgICAgICAg"
        + "ICAgPC94czplbGVtZW50PgogICAgICAgICAgICAgICAgICA8L3hzOnNlcXVlbmNlPgogICAgICAgICAg"
        + "ICAgICAgPC94czpjb21wbGV4VHlwZT4KICAgICAgICAgICAgICA8L3hzOmVsZW1lbnQ+CgogICAgICAg"
        + "ICAgICAgIDx4czplbGVtZW50IG5hbWU9Im1lZGlhcyIgbWluT2NjdXJzPSIwIj4KICAgICAgICAgICAg"
        + "ICAgIDx4czphbm5vdGF0aW9uPgogICAgICAgICAgICAgICAgICA8eHM6ZG9jdW1lbnRhdGlvbiBzb3Vy"
        + "Y2U9ImRlc2NyaXB0aW9uIj4KICAgICAgICAgICAgICAgICAgICBUaGlzIGVsZW1lbnQgZGVmaW5lcyB0"
        + "aGUgcm91dGUgYWxsb3dlZCBtZWRpYSB0eXBlcy4KICAgICAgICAgICAgICAgICAgPC94czpkb2N1bWVu"
        + "dGF0aW9uPgogICAgICAgICAgICAgICAgPC94czphbm5vdGF0aW9uPgogICAgICAgICAgICAgICAgPHhz"
        + "OmNvbXBsZXhUeXBlPgogICAgICAgICAgICAgICAgICA8eHM6c2VxdWVuY2U+CiAgICAgICAgICAgICAg"
        + "ICAgICAgPHhzOmVsZW1lbnQgbmFtZT0ibWVkaWEiIHR5cGU9InhzOnN0cmluZyIgbWF4T2NjdXJzPSJ1"
        + "bmJvdW5kZWQiPgogICAgICAgICAgICAgICAgICAgICAgPHhzOmFubm90YXRpb24+CiAgICAgICAgICAg"
        + "ICAgICAgICAgICAgIDx4czpkb2N1bWVudGF0aW9uIHNvdXJjZT0iZGVzY3JpcHRpb24iPgogICAgICAg"
        + "ICAgICAgICAgICAgICAgICAgIFRoaXMgZWxlbWVudCBkZWZpbmVzIGEgbWVkaWEgdHlwZS4KICAgICAg"
        + "ICAgICAgICAgICAgICAgICAgPC94czpkb2N1bWVudGF0aW9uPgogICAgICAgICAgICAgICAgICAgICAg"
        + "PC94czphbm5vdGF0aW9uPgogICAgICAgICAgICAgICAgICAgIDwveHM6ZWxlbWVudD4KICAgICAgICAg"
        + "ICAgICAgICAgPC94czpzZXF1ZW5jZT4KICAgICAgICAgICAgICAgIDwveHM6Y29tcGxleFR5cGU+CiAg"
        + "ICAgICAgICAgICAgPC94czplbGVtZW50PgoKICAgICAgICAgICAgPC94czphbGw+CiAgICAgICAgICA8"
        + "L3hzOmNvbXBsZXhUeXBlPgogICAgICAgIDwveHM6ZWxlbWVudD4KICAgICAgPC94czpzZXF1ZW5jZT4K"
        + "ICAgIDwveHM6Y29tcGxleFR5cGU+CiAgPC94czplbGVtZW50PgoKPC94czpzY2hlbWE+");
    checkFile(routesXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pjxyb3V0"
        + "ZXMgeG1sbnM9Imh0dHA6Ly93d3cucmVzdG5leHQub3JnL3JvdXRlcyI+PHJvdXRlPjxwYXRoPi90ZXN0"
        + "PC9wYXRoPjxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnJvdXRlLlByb3ZpZGVyOjphbnlNZXRob2RO"
        + "YW1lWW91V2FudDwvcHJvdmlkZXI+PG1ldGhvZHM+PG1ldGhvZD5HRVQ8L21ldGhvZD48bWV0aG9kPlBP"
        + "U1Q8L21ldGhvZD48L21ldGhvZHM+PG1lZGlhcz48bWVkaWE+dGV4dC9wbGFpbjwvbWVkaWE+PG1lZGlh"
        + "PmFwcGxpY2F0aW9uL2pzb248L21lZGlhPjwvbWVkaWFzPjwvcm91dGU+PHJvdXRlPjxwYXRoPi90ZXN0"
        + "L3tuYW1lfTwvcGF0aD48cHJvdmlkZXI+YnIuY29tLnRoaWFndXRlbi5yb3V0ZS5Qcm92aWRlcjo6dGVz"
        + "dDI8L3Byb3ZpZGVyPjwvcm91dGU+PHJvdXRlPjxwYXRoPi90ZXN0L3JlZ2V4L1xcZCs8L3BhdGg+PHBy"
        + "b3ZpZGVyPmJyLmNvbS50aGlhZ3V0ZW4ucm91dGUuUHJvdmlkZXI6OnRlc3QzPC9wcm92aWRlcj48ZW5h"
        + "YmxlPmZhbHNlPC9lbmFibGU+PC9yb3V0ZT48L3JvdXRlcz4=");
    checkFile(routesInvalidXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pjxyb3V0"
        + "ZXMgeG1sbnM9Imh0dHA6Ly93d3cucmVzdG5leHQub3JnL3JvdXRlcyI+PHJvdXRlPjxwYXRoPiQkJCQk"
        + "PC9wYXRoPjxwcm92aWRlcj5sYWxhbGFsYTwvcHJvdmlkZXI+PG1ldGhvZHM+PG1ldGhvZD5HRVQ8L21l"
        + "dGhvZD48bWV0aG9kPmxhbGFsYWxhPC9tZXRob2Q+PC9tZXRob2RzPjxtZWRpYXM+PG1lZGlhPnRleHQv"
        + "cGxhaW48L21lZGlhPjxtZWRpYT5hcHBsaWNhdGlvbi9qc29uPC9tZWRpYT48L21lZGlhcz48L3JvdXRl"
        + "Pjxyb3V0ZT48cGF0aD4vdGVzdC97bmFtZX08L3BhdGg+PHByb3ZpZGVyPmJyLmNvbS50aGlhZ3V0ZW4u"
        + "cm91dGUuUHJvdmlkZXI6OnRlc3QyPC9wcm92aWRlcj48L3JvdXRlPjxyb3V0ZT48cGF0aD4vdGVzdC9y"
        + "ZWdleC9cXGQrPC9wYXRoPjxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnJvdXRlLlByb3ZpZGVyOjp0"
        + "ZXN0MzwvcHJvdmlkZXI+PGVuYWJsZT5mYWxzZTwvZW5hYmxlPjwvcm91dGU+PC9yb3V0ZXM+");
    checkFile(routesFormattedXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pgo8cm91"
        + "dGVzIHhtbG5zPSJodHRwOi8vd3d3LnJlc3RuZXh0Lm9yZy9yb3V0ZXMiPgogICAgPHJvdXRlPgogICAg"
        + "ICAgIDxwYXRoPi90ZXN0PC9wYXRoPgogICAgICAgIDxwcm92aWRlcj5ici5jb20udGhpYWd1dGVuLnJv"
        + "dXRlLlByb3ZpZGVyOjphbnlNZXRob2ROYW1lWW91V2FudDwvcHJvdmlkZXI+CiAgICAgICAgPG1ldGhv"
        + "ZHM+CiAgICAgICAgICAgIDxtZXRob2Q+R0VUPC9tZXRob2Q+CiAgICAgICAgICAgIDxtZXRob2Q+UE9T"
        + "VDwvbWV0aG9kPgogICAgICAgIDwvbWV0aG9kcz4KICAgICAgICA8bWVkaWFzPgogICAgICAgICAgICA8"
        + "bWVkaWE+dGV4dC9wbGFpbjwvbWVkaWE+CiAgICAgICAgICAgIDxtZWRpYT5hcHBsaWNhdGlvbi9qc29u"
        + "PC9tZWRpYT4KICAgICAgICA8L21lZGlhcz4KICAgIDwvcm91dGU+CiAgICA8cm91dGU+CiAgICAgICAg"
        + "PHBhdGg+L3Rlc3Qve25hbWV9PC9wYXRoPgogICAgICAgIDxwcm92aWRlcj5ici5jb20udGhpYWd1dGVu"
        + "LnJvdXRlLlByb3ZpZGVyOjp0ZXN0MjwvcHJvdmlkZXI+CiAgICA8L3JvdXRlPgogICAgPHJvdXRlPgog"
        + "ICAgICAgIDxwYXRoPi90ZXN0L3JlZ2V4L1xcZCs8L3BhdGg+CiAgICAgICAgPHByb3ZpZGVyPmJyLmNv"
        + "bS50aGlhZ3V0ZW4ucm91dGUuUHJvdmlkZXI6OnRlc3QzPC9wcm92aWRlcj4KICAgICAgICA8ZW5hYmxl"
        + "PmZhbHNlPC9lbmFibGU+CiAgICA8L3JvdXRlPgo8L3JvdXRlcz4KCg==");
    checkFile(routesFormattedInvalidXml, ""
        + "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pgo8cm91"
        + "dGVzIHhtbG5zPSJodHRwOi8vd3d3LnJlc3RuZXh0Lm9yZy9yb3V0ZXMiPgogICAgPHJvdXRlPgogICAg"
        + "ICAgIDxwYXRoPmxhbGFsYWxhPC9wYXRoPgogICAgICAgIDxwcm92aWRlcj4kJCQkJCQ8L3Byb3ZpZGVy"
        + "PgogICAgICAgIDxtZXRob2RzPgogICAgICAgICAgICA8bWV0aG9kPmxhbGFsYWxhPC9tZXRob2Q+CiAg"
        + "ICAgICAgICAgIDxtZXRob2Q+UE9TVDwvbWV0aG9kPgogICAgICAgIDwvbWV0aG9kcz4KICAgICAgICA8"
        + "bWVkaWFzPgogICAgICAgICAgICA8bWVkaWE+dGV4dC9wbGFpbjwvbWVkaWE+CiAgICAgICAgICAgIDxt"
        + "ZWRpYT5hcHBsaWNhdGlvbi9qc29uPC9tZWRpYT4KICAgICAgICA8L21lZGlhcz4KICAgIDwvcm91dGU+"
        + "CiAgICA8cm91dGU+CiAgICAgICAgPHBhdGg+L3Rlc3Qve25hbWV9PC9wYXRoPgogICAgICAgIDxwcm92"
        + "aWRlcj5ici5jb20udGhpYWd1dGVuLnJvdXRlLlByb3ZpZGVyOjp0ZXN0MjwvcHJvdmlkZXI+CiAgICA8"
        + "L3JvdXRlPgogICAgPHJvdXRlPgogICAgICAgIDxwYXRoPi90ZXN0L3JlZ2V4L1xcZCs8L3BhdGg+CiAg"
        + "ICAgICAgPHByb3ZpZGVyPmJyLmNvbS50aGlhZ3V0ZW4ucm91dGUuUHJvdmlkZXI6OnRlc3QzPC9wcm92"
        + "aWRlcj4KICAgICAgICA8ZW5hYmxlPmZhbHNlPC9lbmFibGU+CiAgICA8L3JvdXRlPgo8L3JvdXRlcz4K"
        + "Cg==");
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

}

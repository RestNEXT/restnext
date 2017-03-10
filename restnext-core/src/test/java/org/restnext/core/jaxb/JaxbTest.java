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

package org.restnext.core.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Created by thiago on 10/03/17.
 */
public class JaxbTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String xsd = "shiporder.xsd";

  private static final ObjectFactory objectFactory = new ObjectFactory();
  private static final Jaxb jaxb = new Jaxb(Shiporder.class);
  private static final Jaxb jaxbWithSchemaValidation = new Jaxb(xsd, Shiporder.class);

  public Path createXml(String xmlName, boolean formatted, boolean validated)
      throws JAXBException, IOException {

    Shiporder.Item shiporderItem = objectFactory.createShiporderItem();
    shiporderItem.setNote("note");
    shiporderItem.setPrice(BigDecimal.ONE);
    shiporderItem.setQuantity(BigInteger.TEN);
    shiporderItem.setTitle("title");

    Shiporder.Shipto shiporderShipto = objectFactory.createShiporderShipto();
    shiporderShipto.setAddress("address");
    shiporderShipto.setCity("city");
    shiporderShipto.setCountry("country");
    shiporderShipto.setName("name");

    Shiporder shiporder = objectFactory.createShiporder();
    shiporder.setOrderid("orderid");
    shiporder.setOrderperson("orderperson");
    shiporder.setShipto(shiporderShipto);
    shiporder.getItem().add(shiporderItem);

    File xml = temporaryFolder.newFile(xmlName);
    String xmlContent;

    Jaxb xmlJaxb = validated
        ? jaxbWithSchemaValidation
        : jaxb;

    xmlContent = formatted
        ? xmlJaxb.marshal(shiporder) + "\n"
        : xmlJaxb.marshal(shiporder, Collections.singletonMap(
        Marshaller.JAXB_FORMATTED_OUTPUT, false));

    return Files.write(xml.toPath(), xmlContent.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void marshalAndUnmarshalTest() throws JAXBException, IOException {
    marshalAndUnmarshal("xml.xml", false, false);
    marshalAndUnmarshal("xml2.xml", false, true);
    marshalAndUnmarshal("xmlFormatted.xml", true, false);
    marshalAndUnmarshal("xmlFormatted2.xml", true, true);
  }

  private void marshalAndUnmarshal(String xmlName, boolean formatted, boolean validated)
      throws JAXBException, IOException {
    final Class<Shiporder> returnClass = Shiporder.class;

    Path path = createXml(xmlName, formatted, validated);

    // unmarshal with path input
    Shiporder deserialized = jaxb.unmarshal(path, returnClass);
    assertNotNull(deserialized);
    List<Shiporder.Item> route = deserialized.getItem();
    assertEquals(1, route.size());

    // unmarshal with input stream input
    try (InputStream inputStream = Files.newInputStream(path)) {
      deserialized = jaxb.unmarshal(inputStream, returnClass);
      assertNotNull(deserialized);
      route = deserialized.getItem();
      assertEquals(1, route.size());
    }

    // unmarshal with string input
    Collector<CharSequence, ?, String> collector = formatted
        ? Collectors.joining("\n")
        : Collectors.joining();
    String string = Files.lines(path).collect(collector);
    deserialized = jaxb.unmarshal(string, returnClass);
    assertNotNull(deserialized);
    route = deserialized.getItem();
    assertEquals(1, route.size());

    // marshal
    String serialized = jaxb.marshal(deserialized,
        Collections.singletonMap(Marshaller.JAXB_FORMATTED_OUTPUT, formatted));
    assertEquals(string, serialized);
  }

}

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

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

//import org.codehaus.stax2.XMLInputFactory2;
//import org.codehaus.stax2.XMLStreamReader2;
//import javax.jaxb.stream.XMLStreamException;

/**
 * Jaxb utility class.
 *
 * @author Thiago Gutenberg Carvalho da Costa
 */
public final class Jaxb {

    private final JAXBContext context;
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;

    // constructor

    public Jaxb(Class<?>... classes) {
        this(null, classes);
    }

    public Jaxb(String schemaXml, Class<?>... classes) {
        try {
            this.context = JAXBContext.newInstance(classes);
            this.marshaller = context.createMarshaller();
            this.unmarshaller = context.createUnmarshaller();

            if (schemaXml != null) {
                try {
                    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    InputStream xsdStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaXml);
                    Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
                    this.unmarshaller.setSchema(schema);
                } catch (SAXException e) {
                    throw new RuntimeException("Could not load the XSD schema.", e);
                }
            }
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create the JAXBContext instance.", e);
        }
    }

    // getters and setters

    public JAXBContext getContext() {
        return context;
    }

    public Marshaller getMarshaller() {
        return marshaller;
    }

    public Unmarshaller getUnmarshaller() {
        return unmarshaller;
    }

    // convenient methods

    public String marshal(Object object) throws JAXBException {
        return marshal(object, Collections.singletonMap(Marshaller.JAXB_FORMATTED_OUTPUT, true));
    }

    public String marshal(Object object, Map<String, Object> props) throws JAXBException {
        StringWriter sw = new StringWriter();
        if (props != null) {
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                marshaller.setProperty(entry.getKey(), entry.getValue());
            }
        }
        marshaller.marshal(object, sw);
        return sw.toString();
    }

    public <T> T unmarshal(String xml, Class<T> returnClass) throws JAXBException {
        try (StringReader reader = new StringReader(xml)) {
            return returnClass.cast(unmarshaller.unmarshal(reader));
        }
    }

    public <T> T unmarshal(InputStream xml, Class<T> returnClass) throws JAXBException {
        return returnClass.cast(unmarshaller.unmarshal(xml));
    }

    public <T> T unmarshal(Path xml, Class<T> returnClass) throws JAXBException {
        /*
         * Jaxb
         */
        return returnClass.cast(unmarshaller.unmarshal(xml.toFile()));

        /*
         * Jaxb + StAX
         */
//        XMLInputFactory xif = XMLInputFactory.newFactory();
//        XMLStreamReader xsr = null;
//        try {
//            xsr = xif.createXMLStreamReader(new StreamSource(jaxb));
//            return unmarshaller.unmarshal(xsr, returnClass).getValue();
//        }
//        catch (XMLStreamException e) {
//            throw new JAXBException(e);
//        }
//        finally {
//            if (xsr != null) {
//                try {
//                    xsr.close();
//                }
//                catch (XMLStreamException e) {
//                    // NOP
//                }
//            }
//        }

        /*
         * Jaxb + Woodstox
         */
//        XMLInputFactory2 xif2 = (XMLInputFactory2) XMLInputFactory2.newFactory();
//        xif2.configureForLowMemUsage();
//        XMLStreamReader2 xsr2 = null;
//        try {
//            xsr2 = xif2.createXMLStreamReader(jaxb);
//            return unmarshaller.unmarshal(xsr2, returnClass).getValue();
//        } catch (XMLStreamException e) {
//            throw new JAXBException(e);
//        } finally {
//            if (xsr2 != null) {
//                try {
//                    xsr2.close();
//                } catch (XMLStreamException e) {
//                    // NOP
//                }
//            }
//        }
    }
}

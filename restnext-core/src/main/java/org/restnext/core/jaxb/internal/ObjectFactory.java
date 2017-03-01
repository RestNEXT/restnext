
package org.restnext.core.jaxb.internal;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.restnext.core.jaxb.internal package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.restnext.core.jaxb.internal
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Routes }
     * 
     */
    public Routes createRoutes() {
        return new Routes();
    }

    /**
     * Create an instance of {@link Securities }
     * 
     */
    public Securities createSecurities() {
        return new Securities();
    }

    /**
     * Create an instance of {@link Routes.Route }
     * 
     */
    public Routes.Route createRoutesRoute() {
        return new Routes.Route();
    }

    /**
     * Create an instance of {@link Securities.Security }
     * 
     */
    public Securities.Security createSecuritiesSecurity() {
        return new Securities.Security();
    }

    /**
     * Create an instance of {@link Routes.Route.Methods }
     * 
     */
    public Routes.Route.Methods createRoutesRouteMethods() {
        return new Routes.Route.Methods();
    }

    /**
     * Create an instance of {@link Routes.Route.Medias }
     * 
     */
    public Routes.Route.Medias createRoutesRouteMedias() {
        return new Routes.Route.Medias();
    }

}

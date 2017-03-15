# RestNEXT

[![N|Solid](https://avatars3.githubusercontent.com/u/473791?v=3&s=50)](http://netty.io/) Powered By Netty

[![Build Status](https://travis-ci.org/RestNEXT/restnext.svg?branch=master)](https://travis-ci.org/RestNEXT/restnext)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.restnext/restnext/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Corg.restnext)
[![Javadocs](http://www.javadoc.io/badge/org.restnext/restnext.svg)](http://www.javadoc.io/doc/org.restnext/restnext-server)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

RestNext is a framework (extremely simple to use) built on top of Netty framework (4.1.x) with automatic security and route scanning approach for micro services. The RestNext biggest difference is the ability to register/unregister routes and security for routes without having to restart the application server. Where are cross cutting system functionalities.

### Motivation

After several searches on the WWW, I have not found a framework easy to use, highly performative and with the functionality to deploy routes without the need to restart the application server. So I decided to implement my framework.

### Usage

  - Simple usage:
  
    ```java
    class SimpleExample {
        public static void main(String[] args) {
            ServerInitializer
                .route("/", req -> Response.ok("it works").build())
                .route("/ping", req -> Response.ok("pong").build())
                .start();
        }
    }
    ```
    
  - More complete example:
  
    ```java
    class MoreCompleteExample {
        public static void main(String[] args) {
    
            Function<Request, Response> provider = request -> Response.ok("ok").build();
    
            Function<Request, Response> etagProvider = request -> {
                EntityTag entityTag = new EntityTag("contentCalculatedEtagValue");
                return Optional.ofNullable(request.evaluatePreconditions(entityTag))
                        .orElse(Response.ok().tag(entityTag))
                        .build();
            };
    
            Function<Request, Boolean> secureProvider = request -> true;
    
            Route.Mapping[] routes = {
                    Route.Mapping.uri("/1", provider).build(),
                    Route.Mapping.uri("/2", etagProvider).build()
            };
    
            Security.Mapping[] secures = {
                    Security.Mapping.uri("/1", secureProvider).build(),
                    Security.Mapping.uri("/2", secureProvider).build()
            };
    
            ServerInitializer.builder()
                    // automatic registration approach with default path. ($user.dir/route | $user.dir/security)
                    .enableRoutesScan()
                    .enableSecurityRoutesScan()
                    // automatic registration approach with custom path.
                    .enableRoutesScan(Paths.get(System.getProperty("user.home")))
                    .enableSecurityRoutesScan(Paths.get(System.getProperty("user.home"), "sec"))
                    // manual registration approach.
                    .route(uri, etagProvider)
                    .secure(uri, secureProvider)
                    // multiple manual registration approach.
                    .routes(routes)
                    .secures(secures)
                    // build and start the server.
                    .start();
        }
    }
    ```
***NOTE:***

When you enable route/security scanning, the default or custom directory path provided will be monitored to listen to event of creation, modification and deletion of the .jar file. For automatic registration to work properly, the .jar file should contain the following directory structure:

```sh
    /META-INF/route/*.xml
    /META-INF/security/*.xml
```

Each folder (route/security) can contain as many XML files you want, and the name of the XML file can be any name you want.

routes.xml example:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<routes xmlns="http://www.restnext.org/routes">
    <route>
        <path>/test</path>
        <provider>br.com.thiaguten.route.Provider::anyMethodNameYouWant</provider>
        <methods>
            <method>GET</method>
            <method>POST</method>
        </methods>
        <medias>
            <media>text/plain</media>
            <media>application/json</media>
        </medias>
    </route>
    <route>
        <path>/test/{name}</path>
        <provider>br.com.thiaguten.route.Provider::test2</provider>
    </route>
    <route>
        <path>/test/regex/\\d+</path>
        <provider>br.com.thiaguten.route.Provider::test3</provider>
        <enable>false</enable>
    </route>
</routes>
```

The route XML </provider> property value **must** have Method Reference syntax and the class method must be public and static, respecting the following signature:

```java
class Provider {
    public static Response anyMethodNameYouWant(Request request) {
        // process the request and write some response.
        return Response.ok().build();
    }
}
```

security.xml example:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<securities xmlns="http://www.restnext.org/securities">
    <security>
        <path>/test</path>
        <provider>br.com.thiaguten.security.Provider::anyMethodNameYouWant</provider>
    </security>
    <security>
        <path>/test/{name}</path>
        <provider>br.com.thiaguten.security.Provider::test2</provider>
        <enable>false</enable>
    </security>
    <security>
        <path>/test/regex/\\d+</path>
        <provider>br.com.thiaguten.security.Provider::test3</provider>
    </security>
</securities>
```

The security XML </provider> property value **must** have Method Reference syntax and the class method must be public and static, respecting the following signature:

```java
class Provider {
    public static boolean anyMethodNameYouWant(Request request) {
        // validate the request.
        return true;
    }
}
```

### Installation

RestNEXT requires JDK 8 to run.

Download and extract the [latest pre-built release](https://github.com/RestNEXT/restnext/releases).

Maven Artifact:

```xml
<dependency>
    <groupId>org.restnext</groupId>
    <artifactId>restnext-server</artifactId>
    <version>0.3.2</version>
</dependency>
```

### TODOS

 - Write Tests
 - Add Javadoc and Code Comments

#### Fell free to contribute!

#### Special thanks

Thanks to my friend [Wenderson Ferreira de Souza](https://github.com/wendersonferreira) who contributed with some ideas and also encouraged me to start creating this software.

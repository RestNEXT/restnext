# RestNEXT

[![N|Solid](https://avatars3.githubusercontent.com/u/473791?v=3&s=50)](http://netty.io/) Powered By Netty

RestNext is a framework (extremely simple to use) built on top of Netty framework (4.1.x) with automatic security and route scanning approach for micro services. The RestNext biggest difference is the ability to register/unregister routes and security for routes without having to restart the application server. Where are cross cutting system functionalities.

### Motivation

After several searches on the WWW, I have not found a framework easy to use, highly performative and with the functionality to deploy routes without the need to restart the application server. So I decided to implement my framework.

### Usage

  - Simple usage:
    ```java
    public static void main(String[] args) {
        ServerInitializer.route("/", req -> Response.ok("it works".getBytes()).build()).start();
    }
    ```
  - More complete example:
    ```java
    public static void main(String[] args) {

        Function<Request, Response> provider = request -> Response.ok("ok".getBytes()).build();

        Function<Request, Response> etagProvider = request -> {
            EntityTag entityTag = new EntityTag("someCalculatedEtagValue");
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

                .start();
    }
    ```
***NOTE:***

When you enable route/security auto scanning, the default directory path or the custom directory path provided will be monitored to listen to event creation, modification and deletion of .jar files. For automatic registration to work properly, the .jar file should contain the following directory structure:

```sh
    /META-INF/route/*.json
    /META-INF/security/*.json
```
Each folder (route / security) can contain as many JSON files you want, and the name of the JSON file can be any name you want.

Route.json example:
```json
[
  {
    "uri": "/test",
    "provider": "br.com.thiaguten.restnetty.route.Test::process",
    "enable": true,
    "methods": ["GET"]
  },
  {
    "uri": "/test/{name}",
    "provider": "br.com.thiaguten.restnetty.route.Test::process2",
    "enable": true,
    "methods": ["GET"]
  },
  {
    "uri": "/test/regex/\\d+",
    "provider": "br.com.thiaguten.restnetty.route.Test::process3",
    "enable": true,
    "methods": ["GET"],
    "medias": ["text/plain"]
  }
]
```

Security.json example:

```json
[
  {
    "uri": "/test",
    "provider": "br.com.thiaguten.restnetty.security.Test::validate",
    "enable": true
  },
  {
    "uri": "/test/{name}",
    "provider": "br.com.thiaguten.restnetty.security.Test::validate",
    "enable": true
  },
  {
    "uri": "/test/regex/\\d+",
    "provider": "br.com.thiaguten.restnetty.security.Test::validate",
    "enable": true
  }
]
```

The route JSON provider property value **must** be a static method respecting the following signature:

```java
public static Response anyMethodNameYouWant(Request request) {
    // process the request and write some response.
    return ...;
}
```

The security JSON provider property value **must** be a static method respecting the following signature:

```java
public static boolean anyMethodNameYouWant(Request request) {
    // validate the request.
    return ...;
}
```

### Installation

RestNEXT requires JDK 8 to run.

Download and extract the [latest pre-built release](https://github.com/RestNEXT/restnext/releases).

### Todos

 - Write Tests
 - Add Javadoc and Code Comments
 - Publish realease at Maven Central
 - Integrate with Travis CI

License
----

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

**Fell free to contribute!**
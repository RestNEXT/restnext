Description:

RestNext is a framework (extremely simple to use) built on top of Netty framework (4.1.x) with automatic security and route scanning approach for micro services. The RestNext is Java 8 only and their biggest difference is the ability to register/unregister routes and security for routes without having to restart the application server. Where are cross cutting system functionalities.

Motivation:

After several searches on the WWW, I have not found a framework easy to use, highly performative and with the functionality to deploy routes without
the need to restart the application server. So I decided to implement my framework.

Usage:

    Simple example:

    public static void main(String[] args) {
        ServerInitializer.route("/", req -> Response.ok("it works".getBytes()).build()).start();
    }

    More complete example:

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

    NOTE:

    When you enable route/security auto scanning, the default directory path or the custom directory path provided will be monitored to listen to event creation, modification and deletion of .jar files.
    For automatic registration to function properly, the .jar file should contain the following directory structure:

    /META-INF/route/*.json
    /META-INF/security/*.json

    Each folder (route / security) can contain as many JSON files you want, and the name of the JSON file can be any name you want.

    // TODO mostrar um exemplo de cada JSON.

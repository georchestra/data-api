package com.camptocamp.opendata.ogc.features.autoconfigure;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiController;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiDelegate;
import com.camptocamp.opendata.ogc.features.server.impl.CollectionsApiImpl;

@AutoConfiguration
public class ApiAutoConfiguration {

    @Bean
    RouterFunction<ServerResponse> homeController() {
        return route(GET("/"), req -> ServerResponse.temporaryRedirect(URI.create("swagger-ui.html")).build());
    }

    @Bean
    CollectionsApiDelegate collectionsApiDelegate(CollectionRepository repo) {
        return new CollectionsApiImpl(repo);
    }

    @Bean
    CollectionsApiController collectionsApiController(CollectionsApiDelegate delegate) {
        return new CollectionsApiController(delegate);
    }
}

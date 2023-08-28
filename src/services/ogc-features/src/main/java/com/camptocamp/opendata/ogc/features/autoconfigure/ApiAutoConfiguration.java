package com.camptocamp.opendata.ogc.features.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiController;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiDelegate;
import com.camptocamp.opendata.ogc.features.server.config.HomeController;
import com.camptocamp.opendata.ogc.features.server.config.SpringDocConfiguration;
import com.camptocamp.opendata.ogc.features.server.impl.CollectionsApiImpl;

@AutoConfiguration
@Import(SpringDocConfiguration.class)
public class ApiAutoConfiguration {

//    @Bean
//    RouterFunction<ServerResponse> homeController() {
//        return route(GET("/"), req -> ServerResponse.temporaryRedirect(URI.create("swagger-ui.html")).build());
//    }

    @Bean
    HomeController homeController() {
        return new HomeController();
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

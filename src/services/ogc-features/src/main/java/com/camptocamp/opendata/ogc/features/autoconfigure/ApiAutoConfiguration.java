package com.camptocamp.opendata.ogc.features.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.camptocamp.ogc.features.repository.CollectionRepository;
import com.camptocamp.ogc.features.server.api.CollectionsApiController;
import com.camptocamp.ogc.features.server.api.CollectionsApiDelegate;
import com.camptocamp.ogc.features.server.config.HomeController;
import com.camptocamp.ogc.features.server.impl.CollectionsApiImpl;

@AutoConfiguration
public class ApiAutoConfiguration {

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

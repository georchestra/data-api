package com.camptocamp.opendata.ogc.features.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.camptocamp.ogc.features.repository.CollectionRepository;
import com.camptocamp.ogc.features.repository.DataStoreRepository;
import com.camptocamp.opendata.producer.Producers;

@AutoConfiguration
public class BackendAutoConfiguration {

    @Bean
    CollectionRepository collectionRepository(Producers producers) {
        return new DataStoreRepository(producers);
    }
}

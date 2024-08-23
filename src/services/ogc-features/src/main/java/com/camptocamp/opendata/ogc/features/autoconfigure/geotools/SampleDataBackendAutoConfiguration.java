package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.camptocamp.opendata.ogc.features.autoconfigure.postgis.PostgisBackendAutoConfiguration;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreCollectionRepository;
import com.camptocamp.opendata.ogc.features.sampledata.SampleData;
import com.camptocamp.opendata.producer.geotools.FeatureToRecord;

import lombok.extern.slf4j.Slf4j;

/**
 * Supplies an {@link DataStoreCollectionRepository} that works off sample data
 * from the classpath.
 * <p>
 * This is only to be activated with the {@literal sample-data} profile and
 * overrides the {@link CollectionRepository} supplied by
 * {@link PostgisBackendAutoConfiguration}
 */
@AutoConfiguration
@Profile("sample-data")
@Slf4j(topic = "com.camptocamp.opendata.ogc.features.autoconfigure.geotools")
public class SampleDataBackendAutoConfiguration {

    @Bean
    CollectionRepository sampleDataDataStoreCollectionRepository(SampleData dsProvider) {
        log.info("Using geotools sample data CollectionRepository");
        return new DataStoreCollectionRepository(dsProvider, new FeatureToRecord());
    }

    @Bean
    SampleData sampleData() {
        return new SampleData();
    }
}

package com.camptocamp.opendata.ogc.features.autoconfigure.postgis;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.support.DatabaseStartupValidator;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreCollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreProvider;
import com.camptocamp.opendata.producer.geotools.FeatureToRecord;

import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@EnableConfigurationProperties(PostgisSchemasConfiguration.class)
@Profile("postgis")
@Slf4j(topic = "com.camptocamp.opendata.ogc.features.autoconfigure.geotools")
public class PostgisBackendAutoConfiguration implements WebMvcConfigurer {

    @Bean
    CollectionRepository postgisDataStoreCollectionRepository(DataStoreProvider dsProvider) {
        log.info("Using GeoTools PostGIS CollectionRepository");
        return new DataStoreCollectionRepository(dsProvider, new FeatureToRecord());
    }

    @Bean(name = "indexDataStore")
    @DependsOn("databaseStartupValidator")
    SchemaMultiplexingPostgisDataStoreProvider schemaMultiplexingPostgisDataStoreProvider(
            PostgisSchemasConfiguration config, DataSource dataSource) {
        return new SchemaMultiplexingPostgisDataStoreProvider(config, dataSource);
    }

    @Bean
    DatabaseStartupValidator databaseStartupValidator(DataSource dataSource) {
        var dsv = new DatabaseStartupValidator();
        dsv.setDataSource(dataSource);
        return dsv;
    }
}

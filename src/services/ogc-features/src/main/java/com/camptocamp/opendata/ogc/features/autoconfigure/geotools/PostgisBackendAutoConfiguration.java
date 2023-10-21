package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import java.io.IOException;
import java.util.Map;

import javax.sql.DataSource;

import org.geotools.api.data.DataStore;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.support.DatabaseStartupValidator;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreCollectionRepository;
import com.camptocamp.opendata.producer.geotools.FeatureToRecord;

@AutoConfiguration
@Profile("postgis")
public class PostgisBackendAutoConfiguration implements WebMvcConfigurer {

    @Bean
    CollectionRepository postgisDataStoreCollectionRepository(@Qualifier("indexDataStore") DataStore indexStore) {
        return new DataStoreCollectionRepository(indexStore, new FeatureToRecord());
    }

    @Bean(name = "indexDataStore")
    @DependsOn("databaseStartupValidator")
    DataStore postgisDataStore(DataSource dataSource, @Value("${pg.schema:opendataindex}") String schema)
            throws IOException {
        Map<String, ?> params = Map.of(//
                PostgisNGDataStoreFactory.DBTYPE.key, "postgis", //
                PostgisNGDataStoreFactory.DATASOURCE.key, dataSource, //
                PostgisNGDataStoreFactory.SCHEMA.key, schema, //
                PostgisNGDataStoreFactory.PREPARED_STATEMENTS.key, true, //
                PostgisNGDataStoreFactory.ENCODE_FUNCTIONS.key, true, //
                PostgisNGDataStoreFactory.ESTIMATED_EXTENTS.key, true, //
                PostgisNGDataStoreFactory.LOOSEBBOX.key, true//
        );

        PostgisNGDataStoreFactory fac = new PostgisNGDataStoreFactory();
        return fac.createDataStore(params);
    }

    @Bean
    DatabaseStartupValidator databaseStartupValidator(DataSource dataSource) {
        var dsv = new DatabaseStartupValidator();
        dsv.setDataSource(dataSource);
        return dsv;
    }
}

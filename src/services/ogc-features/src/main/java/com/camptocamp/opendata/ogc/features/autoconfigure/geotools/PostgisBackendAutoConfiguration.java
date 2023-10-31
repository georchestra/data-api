package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.geotools.api.data.DataStore;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.support.DatabaseStartupValidator;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreCollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreProvider;
import com.camptocamp.opendata.ogc.features.repository.DefaultDataStoreProvider;
import com.camptocamp.opendata.producer.geotools.FeatureToRecord;

import lombok.NonNull;

@AutoConfiguration
@Profile("postgis")
public class PostgisBackendAutoConfiguration implements WebMvcConfigurer {

    @Bean
    CollectionRepository postgisDataStoreCollectionRepository(DataStoreProvider dsProvider) {
        return new DataStoreCollectionRepository(dsProvider, new FeatureToRecord());
    }

    @Bean(name = "indexDataStore")
    @DependsOn("databaseStartupValidator")
    DataStoreProvider postgisDataStore(DataSource dataSource, @Value("${pg.schema:opendataindex}") String schema)
            throws IOException {
        Map<String, Object> params = Map.of(//
                PostgisNGDataStoreFactory.DBTYPE.key, "postgis", //
                PostgisNGDataStoreFactory.DATASOURCE.key, dataSource, //
                PostgisNGDataStoreFactory.SCHEMA.key, schema, //
                PostgisNGDataStoreFactory.PREPARED_STATEMENTS.key, true, //
                PostgisNGDataStoreFactory.ENCODE_FUNCTIONS.key, true, //
                PostgisNGDataStoreFactory.ESTIMATED_EXTENTS.key, true, //
                PostgisNGDataStoreFactory.LOOSEBBOX.key, true//
        );

        return new PostgisDataStoreProvider(params);
    }

    @Bean
    DatabaseStartupValidator databaseStartupValidator(DataSource dataSource) {
        var dsv = new DatabaseStartupValidator();
        dsv.setDataSource(dataSource);
        return dsv;
    }

    public static class PostgisDataStoreProvider extends DefaultDataStoreProvider {

        public PostgisDataStoreProvider(@NonNull Map<String, Object> connectionParams) {
            super(new HashMap<>(connectionParams));
        }

        public DataSource getDataSource() {
            return (DataSource) super.connectionParams.get(PostgisNGDataStoreFactory.DATASOURCE.key);
        }

        public void setDataSource(DataSource ds) {
            super.connectionParams.put(PostgisNGDataStoreFactory.DATASOURCE.key, ds);
            if (null != super.store) {
                ((JDBCDataStore) super.store).setDataSource(ds);
            }
        }

        @Override
        protected @NonNull DataStore create() {
            PostgisNGDataStoreFactory fac = new PostgisNGDataStoreFactory();
            try {
                return fac.createDataStore(connectionParams);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}

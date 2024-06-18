package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.sql.DataSource;

import org.geotools.api.data.DataStore;
import org.geotools.data.postgis.PostGISDialect;
import org.geotools.data.postgis.PostGISPSDialect;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;
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
import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
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
    DataStoreProvider postgisDataStore(DataSource dataSource, @Value("${postgres.schema:#{null}}") String schema)
            throws IOException {
        Map<String, Object> params = new HashMap<String, Object>(Map.of(//
                PostgisNGDataStoreFactory.DBTYPE.key, "post gis", //
                PostgisNGDataStoreFactory.DATASOURCE.key, dataSource, //
                PostgisNGDataStoreFactory.PREPARED_STATEMENTS.key, true, //
                PostgisNGDataStoreFactory.ENCODE_FUNCTIONS.key, true, //
                PostgisNGDataStoreFactory.ESTIMATED_EXTENTS.key, true, //
                PostgisNGDataStoreFactory.LOOSEBBOX.key, true//
        ));

        if (schema != null) {
            log.info("Schema used : " + schema);
            params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
        }
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
            PostgisNGDataStoreFactory fac = new PostgisNGDataStoreFactory() {
                @Override
                protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
                    return new SchemaUnawarePostGISDialect(dataStore);
                }

                @Override
                protected SQLDialect createSQLDialect(JDBCDataStore dataStore, Map<String, ?> params) {
                    return new SchemaUnawarePostGISDialect(dataStore);
                }
            };
            try {
                return fac.createDataStore(connectionParams);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * A PostGIS dialect that does not rely on the public geometry_columns table to
     * get the SRID of a geometry column. Get the SRID from the geometry in the
     * table instead.
     */
    private static class SchemaUnawarePostGISDialect extends PostGISDialect {
        public SchemaUnawarePostGISDialect(JDBCDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public Integer getGeometrySRID(String schemaName, String tableName, String columnName, Connection cx)
                throws SQLException {
            Integer srid = null;
            try (Statement statement = cx.createStatement()) {

                String sqlStatement = "SELECT ST_SRID(\"" + columnName + "\")::int FROM \"" + tableName + "\" LIMIT 1";
                try (ResultSet result = statement.executeQuery(sqlStatement)) {
                    if (result.next()) {
                        srid = result.getInt(1);
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve information about " + tableName + "." + columnName
                            + " from the geometry_columns table, checking the first geometry instead", e);
                }
            }
            return srid;
        }
    }
}

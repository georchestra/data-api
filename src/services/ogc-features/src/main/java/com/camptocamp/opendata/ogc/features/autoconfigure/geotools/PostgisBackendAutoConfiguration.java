package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import javax.sql.DataSource;

import org.geotools.api.data.DataStore;
import org.geotools.data.postgis.PostGISDialect;
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
        Map<String, Object> params = new HashMap<>(Map.of(//
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
                    String origMessage = Optional.ofNullable(e.getMessage()).orElse("").replaceAll("\\R", " ");
                    LOGGER.log(Level.WARNING, ()->
                    """
                    Failed to retrieve information about %s.%s (%s) \
                    from the geometry_columns table, checking the first geometry instead
                    """.formatted(tableName, columnName, origMessage));
                }
            }
            return srid;
        }

        /**
         * Override default implementation to get the geometry from the expected schema.table
         * Suppose that Postgis version > 1.5
         * Suppose there's only one table with the given name in all schemas
         *
         * @param schemaName The database schema, could be <code>null</code>.
         * @param tableName The table, never <code>null</code>.
         * @param columnName The column name, never <code>null</code>
         * @param cx The database connection.
         * @return The dimension of the geometry column, or 2 if not found.
         * @throws SQLException
         */
        @Override
        public int getGeometryDimension(String schemaName, String tableName, String columnName, Connection cx)
                throws SQLException {
            // first attempt, try with the geometry metadata
            Integer dimension = null;
            try (Statement statement = cx.createStatement()) {
                if (schemaName == null) {
                    String sqlStatement = "select table_schema from information_schema.tables WHERE table_name LIKE '"
                            + tableName + "' LIMIT 1;";
                    LOGGER.log(Level.FINE, "Check table in information schema; {0} ", sqlStatement);
                    try (ResultSet result = statement.executeQuery(sqlStatement)) {

                        if (result.next()) {
                            schemaName = result.getString(1);
                        }
                    } catch (SQLException e) {
                        schemaName = "public";
                    }
                }

                // try geography_columns
                // first look for an entry in geography_columns
                String sqlStatement = "SELECT COORD_DIMENSION FROM GEOGRAPHY_COLUMNS WHERE " //
                        + "F_TABLE_SCHEMA = '" + schemaName + "' " //
                        + "AND F_TABLE_NAME = '" + tableName + "' " //
                        + "AND F_GEOGRAPHY_COLUMN = '" + columnName + "'";
                LOGGER.log(Level.FINE, "Geography srid check; {0} ", sqlStatement);
                try (ResultSet result = statement.executeQuery(sqlStatement)) {

                    if (result.next()) {
                        return result.getInt(1);
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve information about " + schemaName + "." + tableName
                            + "." + columnName + " from the geography_columns table, checking geometry_columns instead",
                            e);
                }
            }

            // fall back on inspection of the first geometry, assuming uniform srid (fair
            // assumption
            // an unpredictable srid makes the table un-queriable)
            if (dimension == null) {
                dimension = getDimensionFromFirstGeo(schemaName, tableName, columnName, cx);
            }

            if (dimension == null) {
                dimension = 2;
            }

            return dimension;
        }
    }
}

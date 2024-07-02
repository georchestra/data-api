package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.geotools.api.data.DataStore;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;
import org.springframework.lang.Nullable;

import com.camptocamp.opendata.ogc.features.repository.DataStoreProvider;
import com.camptocamp.opendata.ogc.features.repository.DefaultDataStoreProvider;
import com.google.common.annotations.VisibleForTesting;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "com.camptocamp.opendata.ogc.features.autoconfigure.geotools")
public class PostgisDataStoreProvider extends DefaultDataStoreProvider {

    PostgisDataStoreProvider(@NonNull Map<String, Object> connectionParams) {
        super(new HashMap<>(connectionParams));
    }

    @VisibleForTesting
    public DataSource getDataSource() {
        return (DataSource) super.connectionParams.get(JDBCDataStoreFactory.DATASOURCE.key);

    }

    public void setDataSource(DataSource ds) {
        super.connectionParams.put(JDBCDataStoreFactory.DATASOURCE.key, ds);
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

    public static DataStoreProvider newInstance(DataSource dataSource, @Nullable String schema) {
        Map<String, Object> params = new HashMap<>(Map.of(//
                PostgisNGDataStoreFactory.DBTYPE.key, "postgis", //
                JDBCDataStoreFactory.DATASOURCE.key, dataSource, //
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
}
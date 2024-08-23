package org.geotools.data.postgis;

import java.util.Map;

import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;

/**
 * Extension to {@link PostgisNGDataStoreFactory} creating data stores that
 * don't rely on the public {@literal geometry_columns} table to get the SRID of
 * a geometry column. Get the SRID from the geometry in the table instead.
 */
public final class SchemaUnawarePostgisNGDataStoreFactory extends PostgisNGDataStoreFactory {
    @Override
    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        return new SchemaUnawarePostGISDialect(dataStore);
    }

    @Override
    protected SQLDialect createSQLDialect(JDBCDataStore dataStore, Map<String, ?> params) {
        return new SchemaUnawarePostGISDialect(dataStore);
    }
}
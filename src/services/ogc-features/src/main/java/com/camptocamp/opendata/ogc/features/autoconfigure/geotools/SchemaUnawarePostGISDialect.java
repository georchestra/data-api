package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Level;

import org.geotools.data.postgis.PostGISDialect;
import org.geotools.jdbc.JDBCDataStore;

/**
 * A PostGIS dialect that does not rely on the public geometry_columns table to
 * get the SRID of a geometry column. Get the SRID from the geometry in the
 * table instead.
 */
class SchemaUnawarePostGISDialect extends PostGISDialect {
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
     * Override default implementation to get the geometry from the expected
     * schema.table Suppose that Postgis version > 1.5 Suppose there's only one
     * table with the given name in all schemas
     *
     * @param schemaName The database schema, could be <code>null</code>.
     * @param tableName  The table, never <code>null</code>.
     * @param columnName The column name, never <code>null</code>
     * @param cx         The database connection.
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
                LOGGER.log(
                        Level.WARNING, "Failed to retrieve information about " + schemaName + "." + tableName + "."
                                + columnName + " from the geography_columns table, checking geometry_columns instead",
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
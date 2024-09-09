package org.geotools.data.postgis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Level;

import org.geotools.jdbc.JDBCDataStore;
import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * A PostGIS dialect that does not rely on the public geometry_columns table to
 * get the SRID of a geometry column. Get the SRID from the geometry in the
 * table instead.
 */
@Slf4j
public class SchemaUnawarePostGISDialect extends PostGISDialect {
    public SchemaUnawarePostGISDialect(JDBCDataStore dataStore) {
        super(dataStore);
    }

    @Override
    public Integer getGeometrySRID(String schemaName, String tableName, String columnName, Connection cx)
            throws SQLException {
        Integer srid = null;
        try (Statement statement = cx.createStatement()) {

            String sqlStatement = """
                    SELECT ST_SRID("%s")::int FROM "%s" LIMIT 1
                    """.formatted(columnName, tableName);
            try (ResultSet result = statement.executeQuery(sqlStatement)) {
                if (result.next()) {
                    srid = result.getInt(1);
                }
            } catch (SQLException e) {
                String origMessage = Optional.ofNullable(e.getMessage()).orElse("").replaceAll("\\R", " ");
                log.warn("""
                        Failed to retrieve information about {}.{} ({}) \
                        from the geometry_columns table, checking the first geometry instead
                        """, tableName, columnName, origMessage);
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
                schemaName = findSchemaName(statement, tableName).orElse("public");
            }

            // try geography_columns first look for an entry in geography_columns
            dimension = findDimensionInGeographyColumns(statement, schemaName, tableName, columnName);
        }

        // fall back on inspection of the first geometry, assuming uniform srid (fair
        // assumption an unpredictable srid makes the table un-queriable)
        if (dimension == null) {
            dimension = getDimensionFromFirstGeo(schemaName, tableName, columnName, cx);
        }

        if (dimension == null || dimension < 2) {
            dimension = 2;
        }

        return dimension;
    }

    @Nullable
    private Integer findDimensionInGeographyColumns(Statement statement, String schemaName, String tableName,
            String columnName) {
        schemaName = escapeSingleQuotes(schemaName);
        tableName = escapeSingleQuotes(tableName);
        columnName = escapeSingleQuotes(columnName);
        String sqlStatement = """
                SELECT COORD_DIMENSION FROM geography_columns
                  WHERE f_table_schema = '%s'
                  AND f_table_name = '%s'
                  AND f_geography_column = '%s'
                """.formatted(schemaName, tableName, columnName);
        log.debug("Geography srid check; {} ", sqlStatement);
        try (ResultSet result = statement.executeQuery(sqlStatement)) {
            if (result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException e) {
            log.warn(
                    "Failed to retrieve information about {}.{}.{} from the geography_columns table, checking geometry_columns instead",
                    schemaName, tableName, columnName, e);
        }
        return null;
    }

    private Optional<String> findSchemaName(Statement statement, String tableName) {
        tableName = escapeSingleQuotes(tableName);
        String sqlStatement = "select table_schema from information_schema.tables WHERE table_name LIKE '%s' LIMIT 1;"
                .formatted(tableName);
        log.debug("Check table in information schema; {} ", sqlStatement);
        try (ResultSet result = statement.executeQuery(sqlStatement)) {
            if (result.next()) {
                return Optional.ofNullable(result.getString(1));
            }
        } catch (SQLException e) {
            log.info("Error obtaining schema name: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Override to apply {@link #escapeSingleQuotes(String)} to query parameters
     * (schema, table, column)
     */
    @Override
    String lookupGeometryType(ResultSet columnMetaData, Connection cx, String gTableName, String gColumnName)
            throws SQLException {

        // grab the information we need to proceed
        String tableName = escapeSingleQuotes(columnMetaData.getString("TABLE_NAME"));
        String columnName = escapeSingleQuotes(columnMetaData.getString("COLUMN_NAME"));
        String schemaName = escapeSingleQuotes(columnMetaData.getString("TABLE_SCHEM"));

        // first attempt, try with the geometry metadata
        String sqlStatement = """
                SELECT TYPE FROM %s WHERE
                F_TABLE_SCHEMA = '%s'
                AND F_TABLE_NAME = '%s'
                AND %s = '%s'
                """.formatted(gTableName, //
                        schemaName, //
                        tableName, //
                        gColumnName, columnName);

        String geomColumn = null;
        try (Statement statement = cx.createStatement();
             ResultSet result = statement.executeQuery(sqlStatement)){

            LOGGER.log(Level.FINE, "Geometry type check; {0} ", sqlStatement);
            if (result.next()) {
                geomColumn = result.getString(1);
            }
        }

        return geomColumn;
    }

    private String escapeSingleQuotes(String identifier) {
        return identifier.replace("'", "''");
    }
}
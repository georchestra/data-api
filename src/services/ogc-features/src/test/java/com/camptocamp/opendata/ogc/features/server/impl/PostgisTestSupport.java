package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.io.FilenameUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgisContainerProvider;

import com.camptocamp.opendata.ogc.features.repository.JdbcDataStoreProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostgisTestSupport {

    private final @NonNull Path tmpdir;
    public JdbcDatabaseContainer<?> postgis;
    private JdbcDataStoreProvider pgDataStoreProvider;

    public static PostgisTestSupport init(Path tmpdir) throws IOException {
        PostgisTestSupport support = new PostgisTestSupport(tmpdir);
        support.beforeAll(tmpdir);
        return support;
    }

    private void beforeAll(Path tmpdir) throws IOException {
        final String initSchema1ScriptHostPath = copyInitScript("/test-data/postgis/opendataindex.sql", tmpdir);
        final String initSchema2ScriptHostPath = copyInitScript("/test-data/postgis/second_schema.sql", tmpdir);

        postgis = (JdbcDatabaseContainer<?>) new PostgisContainerProvider().newInstance()//
                .withDatabaseName("postgis")//
                .withUsername("postigs")//
                .withPassword("postgis")//
                .withFileSystemBind(initSchema1ScriptHostPath,
                        "/docker-entrypoint-initdb.d/11-import-opendataindex-schema.sql")
                .withFileSystemBind(initSchema2ScriptHostPath,
                        "/docker-entrypoint-initdb.d/12-import-secondschema.sql");

        postgis.start();
    }

    public void setUpDefaultSpringDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgis::getJdbcUrl);
        registry.add("spring.datasource.username", postgis::getUsername);
        registry.add("spring.datasource.password", postgis::getPassword);
        registry.add("spring.datasource.hikari.max-lifetime", () -> 1000);
    }

    public void beforeEeach(JdbcDataStoreProvider pgDataStoreProvider) {
        this.pgDataStoreProvider = pgDataStoreProvider;
        reinitDataSource();
        pgDataStoreProvider.reInit();
    }

    private static String copyInitScript(String resourceName, Path tmpdir) throws IOException {
        URL resource = PostgisTestSupport.class.getResource(resourceName);
        assertThat(resource).isNotNull();

        String filename = FilenameUtils.getName(resourceName);
        Path target = tmpdir.resolve(filename);
        try (InputStream in = resource.openStream()) {
            Files.copy(in, target);
        }

        String initScript = target.toAbsolutePath().toString();
        return initScript;
    }

    public void dropTable(String pgSchema, String name) throws SQLException {
        alterDatabase("""
                DROP TABLE "%s"."%s"
                """.formatted(pgSchema, name));
    }

    public void renameTable(String pgSchema, String table, String as) throws SQLException {
        alterDatabase("""
                ALTER TABLE "%s"."%s" RENAME TO "%s"
                """.formatted(pgSchema, table, as));
    }

    public void renameColumn(String pgSchema, String table, String from, String to) throws SQLException {
        alterDatabase("""
                ALTER TABLE "%s"."%s" RENAME COLUMN "%s" TO "%s"
                """.formatted(pgSchema, table, from, to));
    }

    public void dropColumn(String pgSchema, String table, String col) throws SQLException {
        alterDatabase("""
                ALTER TABLE "%s"."%s" DROP COLUMN "%s"
                """.formatted(pgSchema, table, col));
    }

    public void createTestTable(String pgSchema, String tableName) throws SQLException {
        alterDatabase("""
                CREATE TABLE "%s"."%s" (id BIGINT, name TEXT)
                """.formatted(pgSchema, tableName));
    }

    public void alterDatabase(String ddl) throws SQLException {
        // ALTER TABLE needs to grab an exclusive lock on the table, which open
        // connections prevent. close the datasource then
        closeDataSource();

        runSql(ddl);

        // replace the datasource used by the DataStore, but do not dispose the
        // datastore
        resetDataSource();
    }

    public void runSql(String ddl) throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                st.execute(ddl);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(postgis.getJdbcUrl(), postgis.getUsername(), postgis.getPassword());
        return c;
    }

    public void reinitDataSource() {
        closeDataSource();
        resetDataSource();
    }

    public void closeDataSource() {
        closeDataSource(pgDataStoreProvider.getDataSource());
    }

    public void closeDataSource(DataSource ds) {
        HikariDataSource hikariDs = (HikariDataSource) ds;
        hikariDs.close();
    }

    private void resetDataSource() {
        DataSource newDataSource = newDataSource();
        pgDataStoreProvider.setDataSource(newDataSource);
    }

    public DataSource newDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgis.getJdbcUrl());
        config.setUsername(postgis.getUsername());
        config.setPassword(postgis.getPassword());
        DataSource newDataSource = new HikariDataSource(config);
        return newDataSource;
    }
}

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
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.geotools.api.data.DataStore;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@SpringBootTest(classes = OgcFeaturesApp.class)
@ActiveProfiles("postgis")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CollectionsApiImplPostgisIT extends AbstractCollectionsApiImplIT {

    public static JdbcDatabaseContainer<?> postgis;

    private @Autowired DataSource dataSource;

    private @Autowired DataStore pgDataStore;

    static @BeforeAll void setUp(@TempDir Path tmpdir) throws IOException {

        final String initScriptHostPath = copyInitScript(tmpdir);

        postgis = (JdbcDatabaseContainer<?>) new PostgisContainerProvider().newInstance()//
                .withDatabaseName("postgis")//
                .withUsername("postigs")//
                .withPassword("postgis")//
                .withFileSystemBind(initScriptHostPath, "/docker-entrypoint-initdb.d/11-import-sample-data.sql");

        postgis.start();
    }

    private static String copyInitScript(Path tmpdir) throws IOException {
        URL resource = CollectionsApiImplPostgisIT.class.getResource("/test-data/postgis/opendataindex.sql");
        assertThat(resource).isNotNull();

        Path target = tmpdir.resolve("pg-sample-data.sql");
        try (InputStream in = resource.openStream()) {
            Files.copy(in, target);
        }

        String initScript = target.toAbsolutePath().toString();
        return initScript;
    }

    @DynamicPropertySource
    static void postgisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgis::getJdbcUrl);
        registry.add("spring.datasource.username", postgis::getUsername);
        registry.add("spring.datasource.password", postgis::getPassword);
        registry.add("spring.datasource.hikari.max-lifetime", () -> 1000);
    }

    @Test
    public void testGetCollections_survives_schema_change() throws SQLException {
        collectionsApi.getCollections().getBody().getCollections().stream()
                .filter(c -> "locations".equals(c.getTitle())).findFirst().orElseThrow();
        dropColumn();

        collectionsApi.getCollections().getBody().getCollections().stream()
                .filter(c -> "locations".equals(c.getTitle())).findFirst().orElseThrow();
    }

    @Test
    public void testGetCollections_survives_drop_table() throws SQLException {
        var collections = collectionsApi.getCollections().getBody().getCollections().stream().map(Collection::getTitle)
                .collect(Collectors.toSet());

        assertThat(collections).isEqualTo(
                Set.of("locations", "ouvrages-acquis-par-les-mediatheques", "base-sirene-v3", "comptages-velo"));

        dropTable();

        collections = collectionsApi.getCollections().getBody().getCollections().stream().map(Collection::getTitle)
                .collect(Collectors.toSet());
        assertThat(collections)
                .isEqualTo(Set.of("ouvrages-acquis-par-les-mediatheques", "base-sirene-v3", "comptages-velo"));
    }

    @Test
    public void testGetItems_survives_schema_change() throws SQLException {
        MockHttpServletRequest actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");

        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", 10, null, null, null);
        assertThat(response.getBody().getFeatures().toList().size()).isEqualTo(10);

        dropColumn();

        response = collectionsApi.getFeatures("locations", 10, null, null, null);
        assertThat(response.getBody().getFeatures().toList().size()).isEqualTo(10);
    }

    @Test
    public void testGetItem_survives_schema_change() throws SQLException {
        MockHttpServletRequest actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");

        GeodataRecord before = collectionsApi.getFeatures("locations", 1, null, null, null).getBody().getFeatures()
                .toList().get(0);
        assertThat(before.getProperty("year")).isPresent();

        final String id = before.getId();

        dropColumn();

        GeodataRecord after = collectionsApi.getFeature("locations", id).getBody();
        assertThat(after.getProperty("year")).isEmpty();
    }

    private void dropTable() throws SQLException {
        alterDatabase("DROP TABLE opendataindex.locations");
    }

    private void dropColumn() throws SQLException {
        alterDatabase("ALTER TABLE opendataindex.locations DROP COLUMN year");
    }

    private void alterDatabase(String ddl) throws SQLException {
        // ALTER TABLE needs to grab an exclusive lock on the table, which open
        // connections prevent. close the datasource then
        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        hikariDs.close();

        Connection c = DriverManager.getConnection(postgis.getJdbcUrl(), postgis.getUsername(), postgis.getPassword());
        try (Statement st = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                st.execute(ddl);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
            } finally {
                c.setAutoCommit(true);
            }
        } finally {
            c.close();
        }

        // replace the datasource used by the DataStore, but do not dispose the
        // datastore
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgis.getJdbcUrl());
        config.setUsername(postgis.getUsername());
        config.setPassword(postgis.getPassword());
        DataSource newDataSource = new HikariDataSource(config);
        ((JDBCDataStore) pgDataStore).setDataSource(newDataSource);
    }
}

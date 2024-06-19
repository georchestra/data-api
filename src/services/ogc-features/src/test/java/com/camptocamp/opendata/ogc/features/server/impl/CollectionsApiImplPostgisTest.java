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
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;
import com.camptocamp.opendata.ogc.features.autoconfigure.geotools.PostgisDataStoreProvider;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Cleanup;

@SpringBootTest(classes = OgcFeaturesApp.class)
@ActiveProfiles("postgis")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CollectionsApiImplPostgisTest extends AbstractCollectionsApiImplTest {

    public static JdbcDatabaseContainer<?> postgis;

    private @Autowired PostgisDataStoreProvider pgDataStoreProvider;

    private final Set<String> defaultTables = Set.of("locations", "ouvrages-acquis-par-les-mediatheques",
            "base-sirene-v3", "comptages-velo");

    static @BeforeAll void setUp(@TempDir Path tmpdir) throws IOException {

        final String initScriptHostPath = copyInitScript(tmpdir);

        postgis = (JdbcDatabaseContainer<?>) new PostgisContainerProvider().newInstance()//
                .withDatabaseName("postgis")//
                .withUsername("postigs")//
                .withPassword("postgis")//
                .withFileSystemBind(initScriptHostPath, "/docker-entrypoint-initdb.d/11-import-sample-data.sql");

        postgis.start();
    }

    @BeforeEach
    void beforeEeach() {
        reinitDataSource();
        pgDataStoreProvider.reInit();
    }

    @Override
    protected Comparator<GeodataRecord> fidComparator() {
        Comparator<GeodataRecord> fidComparator = (r1, r2) -> {
            long l1 = Long.parseLong(r1.getId());
            long l2 = Long.parseLong(r2.getId());
            return Long.compare(l1, l2);
        };
        return fidComparator;
    }

    private static String copyInitScript(Path tmpdir) throws IOException {
        URL resource = CollectionsApiImplPostgisTest.class.getResource("/test-data/postgis/opendataindex.sql");
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
    void testGetCollections_sees_new_table(TestInfo testInfo) throws SQLException {
        final String table = testInfo.getDisplayName();

        Set<String> collections = getCollectionNames();

        assertThat(collections).doesNotContain(table).containsAll(defaultTables);

        createTestTable(table);

        collections = getCollectionNames();

        assertThat(collections).contains(table).containsAll(defaultTables);
    }

    private Set<String> getCollectionNames() {
        return capabilitiesApi.getCollections().getBody().getCollections().stream().map(Collection::getTitle)
                .collect(Collectors.toSet());
    }

    @Test
    void testGetCollections_survives_schema_change() throws SQLException {
        final String table = "locations";

        Set<String> collections = getCollectionNames();
        assertThat(collections).contains(table);

        renameColumn(table, "city", "ciudad");

        collections = getCollectionNames();
        assertThat(collections).contains(table);

        dropColumn(table, "ciudad");

        collections = getCollectionNames();
        assertThat(collections).contains(table);
    }

    @Test
    void testGetCollections_survives_table_rename(TestInfo testInfo) throws SQLException {
        final String table = testInfo.getDisplayName();
        final String newName = table + "_renamed";
        createTestTable(table);

        Set<String> collections = getCollectionNames();
        assertThat(collections).contains(table);

        renameTable(table, newName);

        collections = getCollectionNames();
        assertThat(collections).doesNotContain(table).contains(newName);
    }

    @Test
    void testGetCollections_survives_drop_table(TestInfo testInfo) throws SQLException {
        final String table = testInfo.getDisplayName();
        createTestTable(table);
        pgDataStoreProvider.reInit();

        var collections = getCollectionNames();

        assertThat(collections).contains(table).containsAll(defaultTables);

        dropTable(table);

        collections = getCollectionNames();
        assertThat(collections).doesNotContain(table).containsAll(defaultTables);
    }

    @Test
    void testGetItems_survives_schema_change() throws SQLException {

        FeaturesQuery query = FeaturesQuery.of("locations").withLimit(10);
        ResponseEntity<FeatureCollection> response = dataApi.getFeatures(query);

        @Cleanup
        Stream<GeodataRecord> features = response.getBody().getFeatures();
        assertThat(features).hasSize(10);

        renameColumn("locations", "year", "año");

        response = dataApi.getFeatures(query);
        @Cleanup
        Stream<GeodataRecord> features1 = response.getBody().getFeatures();
        assertThat(features1).hasSize(10);

        dropColumn("locations", "año");

        response = dataApi.getFeatures(query);

        @Cleanup
        Stream<GeodataRecord> features2 = response.getBody().getFeatures();
        assertThat(features2).hasSize(10);
    }

    @Test
    void testGetItem_survives_schema_change() throws SQLException {
        FeaturesQuery query = FeaturesQuery.of("locations").withLimit(1);

        @Cleanup
        Stream<GeodataRecord> features = dataApi.getFeatures(query).getBody().getFeatures();
        GeodataRecord before = features.findFirst().orElseThrow();
        assertThat(before.getProperty("number")).isPresent();
        final String id = before.getId();

        renameColumn("locations", "number", "número");

        GeodataRecord after = dataApi.getFeature("locations", id).getBody();
        assertThat(after.getProperty("number")).isEmpty();
        assertThat(after.getProperty("número")).isPresent();

        dropColumn("locations", "número");

        after = dataApi.getFeature("locations", id).getBody();
        assertThat(after.getProperty("número")).isEmpty();
    }

    private void dropTable(String name) throws SQLException {
		alterDatabase("""
				DROP TABLE opendataindex."%s"
				""".formatted(name));
	}

    private void renameTable(String table, String as) throws SQLException {
		alterDatabase("""
				ALTER TABLE opendataindex."%s" RENAME TO "%s"
				""".formatted(table, as));
	}

    private void renameColumn(String table, String from, String to) throws SQLException {
		alterDatabase("""
				ALTER TABLE opendataindex."%s" RENAME COLUMN "%s" TO "%s"
				""".formatted(table, from, to));
	}

    private void dropColumn(String table, String col) throws SQLException {
		alterDatabase("""
				ALTER TABLE opendataindex.%s DROP COLUMN "%s"
				""".formatted(table, col));
	}

    private void createTestTable(String tableName) throws SQLException {
		alterDatabase("""
				CREATE TABLE opendataindex."%s" (id BIGINT, name TEXT)
				""".formatted(tableName));
	}

    private void alterDatabase(String ddl) throws SQLException {
        // ALTER TABLE needs to grab an exclusive lock on the table, which open
        // connections prevent. close the datasource then
        closeDataSource();

        Connection c = DriverManager.getConnection(postgis.getJdbcUrl(), postgis.getUsername(), postgis.getPassword());
        try (Statement st = c.createStatement()) {
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
        } finally {
            c.close();
        }

        // replace the datasource used by the DataStore, but do not dispose the
        // datastore
        resetDataSource();
    }

    private void reinitDataSource() {
        closeDataSource();
        resetDataSource();
    }

    private void closeDataSource() {
        HikariDataSource hikariDs = (HikariDataSource) pgDataStoreProvider.getDataSource();
        hikariDs.close();
    }

    private void resetDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgis.getJdbcUrl());
        config.setUsername(postgis.getUsername());
        config.setPassword(postgis.getPassword());
        DataSource newDataSource = new HikariDataSource(config);
        pgDataStoreProvider.setDataSource(newDataSource);
    }
}

package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.repository.JdbcDataStoreProvider;

import lombok.Cleanup;

@SpringBootTest(classes = OgcFeaturesApp.class, properties = { //
        // configure the opendataindex schema to disable prefixing, there's a single
        // schema in this test suite
        "postgis.schemas.include[0].schema=opendataindex", //
        "postgis.schemas.include[0].prefix-tables=false",//
})
@ActiveProfiles("postgis")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CollectionsApiImplPostgisIT extends AbstractCollectionsApiImplTest {

    static PostgisTestSupport support;

    private @Autowired JdbcDataStoreProvider pgDataStoreProvider;

    private final String pgSchema = "opendataindex";

    private final Set<String> defaultTables = Set.of("locations", "ouvrages-acquis-par-les-mediatheques",
            "base-sirene-v3", "comptages-velo");

    @BeforeAll
    static void setUpContainer(@TempDir Path tmpdir) throws IOException {
        support = PostgisTestSupport.init(tmpdir);
    }

    @DynamicPropertySource
    static void setUpPostgisProperties(DynamicPropertyRegistry registry) {
        support.setUpDefaultSpringDataSource(registry);
    }

    @BeforeEach
    void beforeEeach() {
        support.beforeEeach(pgDataStoreProvider);
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

    @Test
    void testGetCollections_sees_new_table(TestInfo testInfo) throws SQLException {
        final String table = testInfo.getDisplayName();

        Set<String> collections = getCollectionNames();

        assertThat(collections).doesNotContain(table).containsAll(defaultTables);

        support.createTestTable(pgSchema, table);

        collections = getCollectionNames();

        assertThat(collections).contains(table).containsAll(defaultTables);
    }

    private Set<String> getCollectionNames() {
        return capabilitiesApi.getCollections().getBody().getCollections().stream().map(Collection::getTitle)
                .collect(Collectors.toSet());
    }

    @Test
    void testGetCollections_survives_table_schema_change() throws SQLException {
        final String table = "locations";

        Set<String> collections = getCollectionNames();
        assertThat(collections).contains(table);

        support.renameColumn(pgSchema, table, "city", "ciudad");

        collections = getCollectionNames();
        assertThat(collections).contains(table);

        support.dropColumn(pgSchema, table, "ciudad");

        collections = getCollectionNames();
        assertThat(collections).contains(table);
    }

    @Test
    void testGetCollections_survives_table_rename(TestInfo testInfo) throws SQLException {
        final String table = testInfo.getDisplayName();
        final String newName = table + "_renamed";
        support.createTestTable(pgSchema, table);

        Set<String> collections = getCollectionNames();
        assertThat(collections).contains(table);

        support.renameTable(pgSchema, table, newName);

        collections = getCollectionNames();
        assertThat(collections).doesNotContain(table).contains(newName);
    }

    @Test
    void testGetCollections_survives_drop_table(TestInfo testInfo) throws SQLException {
        final String table = testInfo.getDisplayName();
        support.createTestTable(pgSchema, table);
        pgDataStoreProvider.reInit();

        var collections = getCollectionNames();

        assertThat(collections).contains(table).containsAll(defaultTables);

        support.dropTable(pgSchema, table);

        collections = getCollectionNames();
        assertThat(collections).doesNotContain(table).containsAll(defaultTables);
    }

    @Test
    void testGetItems_survives_table_schema_change() throws SQLException {

        FeaturesQuery query = FeaturesQuery.of("locations").withLimit(10);
        ResponseEntity<FeatureCollection> response = dataApi.getFeatures(query);

        @Cleanup
        Stream<GeodataRecord> features = response.getBody().getFeatures();
        assertThat(features).hasSize(10);

        support.renameColumn(pgSchema, "locations", "year", "año");

        response = dataApi.getFeatures(query);
        @Cleanup
        Stream<GeodataRecord> features1 = response.getBody().getFeatures();
        assertThat(features1).hasSize(10);

        support.dropColumn(pgSchema, "locations", "año");

        response = dataApi.getFeatures(query);

        @Cleanup
        Stream<GeodataRecord> features2 = response.getBody().getFeatures();
        assertThat(features2).hasSize(10);
    }

    @Test
    void testGetItem_survives_table_schema_change() throws SQLException {
        FeaturesQuery query = FeaturesQuery.of("locations").withLimit(1);

        @Cleanup
        Stream<GeodataRecord> features = dataApi.getFeatures(query).getBody().getFeatures();
        GeodataRecord before = features.findFirst().orElseThrow();
        assertThat(before.getProperty("number")).isPresent();
        final String id = before.getId();

        support.renameColumn(pgSchema, "locations", "number", "número");

        GeodataRecord after = dataApi.getFeature("locations", id).getBody();
        assertThat(after.getProperty("number")).isEmpty();
        assertThat(after.getProperty("número")).isPresent();

        support.dropColumn(pgSchema, "locations", "número");

        after = dataApi.getFeature("locations", id).getBody();
        assertThat(after.getProperty("número")).isEmpty();
    }
}

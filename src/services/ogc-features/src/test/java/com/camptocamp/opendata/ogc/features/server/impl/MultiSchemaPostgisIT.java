package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;
import com.camptocamp.opendata.ogc.features.autoconfigure.postgis.PostgisSchemasConfiguration;
import com.camptocamp.opendata.ogc.features.autoconfigure.postgis.PostgisSchemasConfiguration.SchemaConfiguration;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.Collections;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.repository.JdbcDataStoreProvider;

import lombok.Cleanup;

/**
 * Integration test for the postgis profile, where there are multiple postgresql
 * schemas to serve data from.
 * 
 * @see PostgisSchemasConfiguration
 */
@SpringBootTest(classes = OgcFeaturesApp.class, properties = { //
        "postgis.schemas.include[0].schema=*", //
        "postgis.schemas.include[1].schema=opendataindex", //
        "postgis.schemas.include[1].alias=aliased", //
        // lower schema names cache TTL to 1s
        "postgis.schemas.refresh.interval=PT1S" })
@ActiveProfiles("postgis")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MultiSchemaPostgisIT {

    static PostgisTestSupport support;

    protected @Autowired CapabilitiesApiImpl capabilitiesApi;
    protected @Autowired DataApiImpl dataApi;
    protected @Autowired NativeWebRequest req;
    private @Autowired JdbcDataStoreProvider pgDataStoreProvider;
    private @Autowired PostgisSchemasConfiguration schemasConfig;

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

    /**
     * These tables exist in both schemas, "opendataindex" and
     * "ville_Villeneuve-d'Ascq"
     */
    private final Set<String> tableNamesCommonToBothSchemas = Set.of("locations",
            "ouvrages-acquis-par-les-mediatheques", "base-sirene-v3", "comptages-velo");

    protected MockHttpServletRequest actualRequest;

    @BeforeEach
    void prepRequest() {
        actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");
    }

    @ParameterizedTest(name = "[{index}] schema: {argumentsWithNames}")
    @ValueSource(strings = { "aliased", "ville_Villeneuve-d'Ascq" })
    void getCollectionsIncludesAllSchemas(String schemaOrAlias) {

        var expected = tableNamesCommonToBothSchemas.stream().map(table -> "%s:%s".formatted(schemaOrAlias, table))
                .toList();

        ResponseEntity<Collections> response = capabilitiesApi.getCollections();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Collections body = response.getBody();

        var actual = body.getCollections().stream().map(Collection::getTitle).collect(Collectors.toSet());
        assertThat(actual).containsAll(expected);
    }

    @ParameterizedTest(name = "[{index}] schema: {argumentsWithNames}")
    @ValueSource(strings = { "aliased", "ville_Villeneuve-d'Ascq" })
    void testGetItems(String schemaOrAlias) {
        String typeName = schemaOrAlias + ":ouvrages-acquis-par-les-mediatheques";
        FeaturesQuery query = FeaturesQuery.of(typeName).withLimit(10);
        ResponseEntity<FeatureCollection> response = dataApi.getFeatures(query);

        @Cleanup
        Stream<GeodataRecord> features = response.getBody().getFeatures();
        assertThat(features).isNotEmpty();
    }

    @Test
    void testSeesNewSchemasWithinTimeout() throws SQLException {
        Set<String> schemasOrAliases = getSchemasFromCollections();
        assertThat(schemasOrAliases).containsExactlyInAnyOrder("aliased", "ville_Villeneuve-d'Ascq");

        support.alterDatabase("CREATE SCHEMA test_new_schema;");
        support.runSql("SELECT * INTO test_new_schema.new_locations FROM opendataindex.locations");

        Duration updateInterval = schemasConfig.getRefresh().getInterval();
        try {
            await().atMost(updateInterval.plusSeconds(1))//
                    .untilAsserted(() -> //
                    assertThat(getSchemasFromCollections())//
                            .containsExactlyInAnyOrder("test_new_schema", "aliased", "ville_Villeneuve-d'Ascq"));
        } finally {
            support.alterDatabase("DROP SCHEMA test_new_schema CASCADE;");
        }
    }

    @Test
    void testDiscardsDeletedSchemasWithinTimeout() throws SQLException {
        support.alterDatabase("CREATE SCHEMA test_delete_schema;");
        support.runSql("SELECT * INTO test_delete_schema.new_locations FROM opendataindex.locations");

        Duration updateInterval = schemasConfig.getRefresh().getInterval();

        await().atMost(updateInterval.plusSeconds(1))//
                .untilAsserted(() -> //
                assertThat(getSchemasFromCollections()).contains("test_delete_schema"));

        support.alterDatabase("DROP SCHEMA test_delete_schema CASCADE;");

        await().atMost(updateInterval.plusSeconds(1))//
                .untilAsserted(() -> //
                assertThat(getSchemasFromCollections())//
                        .doesNotContain("test_delete_schema")
                        .containsExactlyInAnyOrder("aliased", "ville_Villeneuve-d'Ascq"));
    }

    @Test
    void testDiscardsDeletedSchemasTimeoutDisabled() throws SQLException {
        support.alterDatabase("CREATE SCHEMA test_delete_schema;");
        support.runSql("SELECT * INTO test_delete_schema.new_locations FROM opendataindex.locations");

        this.schemasConfig.getRefresh().setEnabled(false);
        try {
            assertThat(getSchemasFromCollections()).contains("test_delete_schema");

            support.alterDatabase("DROP SCHEMA test_delete_schema CASCADE;");
            assertThat(getSchemasFromCollections())//
                    .doesNotContain("test_delete_schema");
        } finally {
            this.schemasConfig.getRefresh().setEnabled(true);
        }
    }

    @Test
    void testOneSchemaWithoutPrefixing() throws SQLException {

        SchemaConfiguration opendataindexConfig = this.schemasConfig.getInclude().get(1);
        assertThat(opendataindexConfig.getSchema()).isEqualTo("opendataindex");

        // disable refresh so we don't need to await()
        this.schemasConfig.getRefresh().setEnabled(false);
        opendataindexConfig.setPrefixTables(false);
        try {
            for (String collectionId : tableNamesCommonToBothSchemas) {
                ResponseEntity<Collection> colresponse = capabilitiesApi.describeCollection(collectionId);

                assertThat(colresponse.getStatusCode()).as("unaliased collection %s not found".formatted(collectionId))
                        .isEqualTo(HttpStatus.OK);

                FeaturesQuery query = FeaturesQuery.of(collectionId).withLimit(1);
                ResponseEntity<FeatureCollection> features = dataApi.getFeatures(query);
                assertThat(features.getStatusCode()).as("unaliased collection %s not found".formatted(collectionId))
                        .isEqualTo(HttpStatus.OK);
            }

        } finally {
            this.schemasConfig.getRefresh().setEnabled(true);
            opendataindexConfig.setPrefixTables(true);
        }
    }

    @Test
    void testQueryForSchemaInsteadOfAlias() throws SQLException {

        SchemaConfiguration opendataindexConfig = this.schemasConfig.getInclude().get(1);
        assertThat(opendataindexConfig.getSchema()).isEqualTo("opendataindex");
        assertThat(opendataindexConfig.getAlias()).isEqualTo("aliased");

        ResponseEntity<Collection> colresponse;
        colresponse = capabilitiesApi.describeCollection("opendataindex:locations");
        assertThat(colresponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        colresponse = capabilitiesApi.describeCollection("aliased:locations");
        assertThat(colresponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testNoExplicitDefaultConfigFallsBackToDefault() throws SQLException {

        SchemaConfiguration defaults = this.schemasConfig.getInclude().remove(0);
        assertThat(defaults.getSchema()).isEqualTo("*");
        this.schemasConfig.clearCache();
        try {
            pgDataStoreProvider.reInit();

            ResponseEntity<Collection> colresponse;
            colresponse = capabilitiesApi.describeCollection("ville_Villeneuve-d'Ascq:locations");
            assertThat(colresponse.getStatusCode())
                    .as("no explicit nor default config for schema should default to prefix by schema")
                    .isEqualTo(HttpStatus.OK);
        } finally {
            this.schemasConfig.getInclude().add(0, defaults);
            this.schemasConfig.clearCache();
            pgDataStoreProvider.reInit();
        }
    }

    private Set<String> getSchemasFromCollections() {
        RequestContextHolder.setRequestAttributes(new ServletWebRequest(actualRequest));
        List<Collection> collections = capabilitiesApi.getCollections().getBody().getCollections();
        return collections.stream().map(Collection::getId).map(this::getPrefix).collect(Collectors.toSet());
    }

    private String getPrefix(String collectionId) {
        return schemasConfig.extractPrefix(collectionId);
    }
}

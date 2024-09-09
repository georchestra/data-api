package com.camptocamp.opendata.ogc.features.autoconfigure.postgis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.camptocamp.opendata.ogc.features.server.impl.PostgisTestSupport;

class PostgisBackendAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("spring.profiles.active=postgis")
            .withConfiguration(AutoConfigurations.of(PostgisBackendAutoConfiguration.class));

    static PostgisTestSupport support;

    @BeforeAll
    static void setUpContainer(@TempDir Path tmpdir) throws IOException {
        support = PostgisTestSupport.init(tmpdir);
    }

    DataSource dataSource;

    @BeforeEach
    void setUpDataSource() {
        dataSource = support.newDataSource();
        runner = runner.withBean(DataSource.class, () -> dataSource);
    }

    @AfterEach
    void closeDataSource() {
        support.closeDataSource(dataSource);
    }

    @Test
    void testPostgisProfileDisabled() {
        runner.withPropertyValues("spring.profiles.active=default")
                .run(context -> assertThat(context).hasNotFailed().as("should only be enabled with the postgis profile")
                        .doesNotHaveBean(PostgisSchemasConfiguration.class)
                        .doesNotHaveBean(SchemaMultiplexingPostgisDataStoreProvider.class));
    }

    @Test
    void testPostgisProfileEnabled() {
        runner.withPropertyValues("spring.profiles.active=postgis")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(PostgisSchemasConfiguration.class)
                        .hasSingleBean(SchemaMultiplexingPostgisDataStoreProvider.class));
    }

    @Test
    void testDefaultSchemaConfigProperties() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var config = context.getBean(PostgisSchemasConfiguration.class);
            assertThat(config).isEqualTo(PostgisSchemasConfiguration.defaultConfig());
        });
    }

    @Test
    void testSchemaConfigValidateRefresh() {
        runner.withPropertyValues("postgis.schemas.refresh.interval=badFormat").run(context -> assertThat(context)
                .hasFailed().getFailure().hasRootCauseMessage("'badFormat' is not a valid duration"));
    }

    @Test
    void testSchemaConfigValidateNoWildcardUnprefixed() {
        runner.withPropertyValues(//
                "postgis.schemas.include[0].schema=*", //
                "postgis.schemas.include[0].prefix-tables=false"//
        ).run(context -> assertThat(context).hasFailed().getFailure()
                .hasRootCauseMessage("The '*' schema wildcard can't have prefix-tables=false"));
    }

    @Test
    void testSchemaConfigValidateNoMoreThanOneUnprefixedSchema() {
        runner.withPropertyValues(//
                "postgis.schemas.include[0].schema=schema1", //
                "postgis.schemas.include[0].prefix-tables=false", //
                "postgis.schemas.include[1].schema=schema2", //
                "postgis.schemas.include[1].prefix-tables=false"//
        ).run(context -> assertThat(context).hasFailed().getFailure()
                .hasRootCauseMessage("Multiple schemas configured with prefix-tables=false: schema1,schema2"));
    }
}

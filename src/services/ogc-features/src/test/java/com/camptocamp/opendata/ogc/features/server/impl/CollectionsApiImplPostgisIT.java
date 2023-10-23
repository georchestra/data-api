package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;

@SpringBootTest(classes = OgcFeaturesApp.class)
@ActiveProfiles("postgis")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CollectionsApiImplPostgisIT extends AbstractCollectionsApiImplIT {

    public static JdbcDatabaseContainer<?> postgis;

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
    }
}

package com.camptocamp.opendata.ogc.features.autoconfigure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.geotools.data.DataStore;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.util.FileSystemUtils;

import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreRepository;
import com.google.common.io.ByteStreams;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Supplies an {@link DataStoreRepository} that works off sample data from the
 * classpath.
 * <p>
 * This is only to be activated with the {@literal sample-data} profile and
 * overrides the {@link CollectionRepository} supplied by
 * {@link BackendAutoConfiguration}
 */
@AutoConfiguration
@Profile("sample-data")
@Slf4j
public class SampleDataBackendAutoConfiguration {

//	@Bean
//	IndexedReader sampleDataIndexedReader(@NonNull GeoToolsDataReader gtReader) throws IOException {
//		return new SampleDataReader(gtReader);
//	}

    @Bean
    DataStore indexDataStore(SampleData sampleData) throws IOException {
        return sampleData.getDataStore();
    }

    @Bean
    SampleData sampleData() throws IOException {
        return new SampleData();
    }

    /**
     * Extracts sample data to a temporary directory and deletes at context
     * shutdown, since the geotools CSV datastore does not support URL resources,
     * only Files
     */
    private static class SampleData implements DisposableBean {

        private Path tempDirectory = Files.createTempDirectory("ogc-features-sample-data");
        private final @Getter DataStore dataStore;

        SampleData() throws IOException {
            dataStore = new MemoryDataStore();
            log.info("Extracting sample data to {}", tempDirectory);

            File sd = copyToTempDir("sample-datasets.gpkg");

            final GeoPkgDataStoreFactory factory = new GeoPkgDataStoreFactory();
            final Map<String, ?> params = Map.of(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg",
                    GeoPkgDataStoreFactory.DATABASE.key, sd);

            JDBCDataStore sdds = factory.createDataStore(params);
            try {
                String[] sampleDatasources = { "locations", "base-sirene-v3", "comptages-velo" };

                for (int i = 0; i < sampleDatasources.length; ++i) {
                    ((MemoryDataStore) dataStore)
                            .addFeatures(sdds.getFeatureSource(sampleDatasources[i]).getFeatures());
                }
            } finally {
                sdds.dispose();
            }
        }

        @Override
        public void destroy() throws Exception {
            dataStore.dispose();
            if (tempDirectory != null && Files.isDirectory(tempDirectory)) {
                log.info("Deleting sample data directory {}", tempDirectory);
                FileSystemUtils.deleteRecursively(tempDirectory);
            }
        }

        private File copyToTempDir(String fileName) throws IOException {
            String uri = "/sample-data/" + fileName;

            Path target = tempDirectory.resolve(fileName);
            try (var from = getClass().getResourceAsStream(uri); var to = new FileOutputStream(target.toFile())) {

                Objects.requireNonNull(from, () -> "resource " + uri + " not found");
                ByteStreams.copy(from, to);
            }
            return target.toFile();
        }
    }
}

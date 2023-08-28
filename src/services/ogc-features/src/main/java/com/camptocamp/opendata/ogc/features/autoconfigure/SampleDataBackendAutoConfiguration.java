package com.camptocamp.opendata.ogc.features.autoconfigure;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.geotools.data.DataStore;
import org.geotools.data.FileDataStore;
import org.geotools.data.csv.CSVDataStoreFactory;
import org.geotools.data.memory.MemoryDataStore;
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

        private Path directory;
        private final @Getter DataStore dataStore;

        SampleData() throws IOException {
            dataStore = new MemoryDataStore();

            directory = Files.createTempDirectory("ogc-features-sample-data");
            log.info("Extracting sample data to {}", directory);
            Path locations = copy("locations.csv");

            Map<String, ?> params = Map.of(//
                    CSVDataStoreFactory.URL_PARAM.key, locations.toUri().toURL(), //
                    CSVDataStoreFactory.SEPERATORCHAR.key, ",", //
                    CSVDataStoreFactory.STRATEGYP.key, CSVDataStoreFactory.SPECIFC_STRATEGY, //
                    CSVDataStoreFactory.LATFIELDP.key, "LAT", //
                    CSVDataStoreFactory.LnGFIELDP.key, "LON");

            FileDataStore csvDs = new CSVDataStoreFactory().createDataStore(params);
            ((MemoryDataStore) dataStore).addFeatures(csvDs.getFeatureSource("locations").getFeatures());

            Path base_sirene_v3 = copy("base-sirene-v3.csv");
            params = Map.of(//
                    CSVDataStoreFactory.URL_PARAM.key, base_sirene_v3.toUri().toURL(), //
                    CSVDataStoreFactory.SEPERATORCHAR.key, ";");

            csvDs = new CSVDataStoreFactory().createDataStore(params);
            ((MemoryDataStore) dataStore).addFeatures(csvDs.getFeatureSource("base-sirene-v3").getFeatures());

            Path comptages_velo = copy("comptages-velo.csv");
            params = Map.of(//
                    CSVDataStoreFactory.URL_PARAM.key, comptages_velo.toUri().toURL(), //
                    CSVDataStoreFactory.SEPERATORCHAR.key, ";");

            csvDs = new CSVDataStoreFactory().createDataStore(params);
            ((MemoryDataStore) dataStore).addFeatures(csvDs.getFeatureSource("comptages-velo").getFeatures());
        }

        @Override
        public void destroy() throws Exception {
            dataStore.dispose();
            if (directory != null && Files.isDirectory(directory)) {
                log.info("Deleting sample data directory {}", directory);
                FileSystemUtils.deleteRecursively(directory);
            }
        }

        private Path copy(String fileName) throws IOException {
            String uri = "/sample-data/" + fileName;

            Path target = directory.resolve(fileName);
            try (var from = getClass().getResourceAsStream(uri); var to = new FileOutputStream(target.toFile())) {

                Objects.requireNonNull(from, () -> "resource " + uri + " not found");
                ByteStreams.copy(from, to);
            }
            return target;
        }
    }
}

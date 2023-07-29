package com.camptocamp.opendata.ogc.features.autoconfigure;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.util.FileSystemUtils;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.DataSource;
import com.camptocamp.opendata.ogc.features.reader.IndexedReader;
import com.camptocamp.opendata.producer.geotools.GeoToolsDataReader;
import com.google.common.io.ByteStreams;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@Profile("sample-data")
@Slf4j
public class SampleDataBackendAutoConfiguration {

    @Bean
    IndexedReader sampleDataIndexedReader(@NonNull GeoToolsDataReader gtReader) throws IOException {
        return new SampleDataReader(gtReader);
    }

    /**
     * Extracts sample data to a temporary directory and deletes at context
     * shutdown, since the geotools CSV datastore does not support URL resources,
     * only Files
     */
    private static class SampleDataReader extends IndexedReader implements DisposableBean {

        private Path directory;

        SampleDataReader(GeoToolsDataReader gtReader) throws IOException {
            super(gtReader);

            directory = Files.createTempDirectory("ogc-features-sample-data");
            log.info("Extracting sample data to {}", directory);
            copy("locations.csv");
        }

        @Override
        protected DataQuery toGtQuery(@NonNull DataQuery query) {
            String layerName = query.getLayerName();
            Objects.requireNonNull(layerName, "layerName");
            Path path = directory.resolve(layerName + ".csv");
            URI uri = path.toUri();
            return query.withSource(DataSource.fromUri(uri));
        }

        @Override
        public void destroy() throws Exception {
            if (directory != null && Files.isDirectory(directory)) {
                log.info("Deleting sample data directory {}", directory);
                FileSystemUtils.deleteRecursively(directory);
            }
        }

        private void copy(String fileName) throws IOException {
            String uri = "/sample-data/" + fileName;

            try (var from = getClass().getResourceAsStream(uri);
                    var to = new FileOutputStream(directory.resolve(fileName).toFile())) {

                Objects.requireNonNull(from, () -> "resource " + uri + " not found");
                ByteStreams.copy(from, to);
            }
        }
    }
}

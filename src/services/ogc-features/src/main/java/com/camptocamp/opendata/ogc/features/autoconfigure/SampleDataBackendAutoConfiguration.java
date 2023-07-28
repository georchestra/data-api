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
import com.camptocamp.opendata.ogc.features.reader.IndexedReader;
import com.camptocamp.opendata.producer.geotools.GeoToolsDataReader;
import com.google.common.io.ByteStreams;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@Profile("sample-data")
@Slf4j
public class SampleDataBackendAutoConfiguration {

	@Bean
	SampleDataContainer sampleDataContainer() {
		return new SampleDataContainer();
	}

	@Bean
	IndexedReader sampleDataIndexedReader(@NonNull GeoToolsDataReader gtReader, SampleDataContainer sampleData) {
		return new IndexedReader(gtReader) {

			@Override
			protected DataQuery toGtQuery(@NonNull DataQuery query) {
				URI uri = sampleData.datasetUri(query.getLayerName());
				return DataQuery.fromUri(uri);
			}
		};
	}

	/**
	 * Extracts sample data to a temporary directory and deletes at context
	 * shutdown, since the geotools CSV datastore does not support URL resources,
	 * only Files
	 */
	private static class SampleDataContainer implements DisposableBean {
		private Path directory;

		@PostConstruct
		void extract() throws IOException {
			directory = Files.createTempDirectory("ogc-features-sample-data");
			log.info("Extracting sample data to {}", directory);
			copy("locations.csv");
		}

		public URI datasetUri(String layerName) {
			return directory.resolve(layerName).toUri();
		}

		private void copy(String fileName) throws IOException {
			String uri = "/sample-data/" + fileName;

			try (var from = getClass().getResourceAsStream(uri);
					var to = new FileOutputStream(directory.resolve(fileName).toFile())) {

				Objects.requireNonNull(from, () -> "resource " + uri + " not found");
				ByteStreams.copy(from, to);
			}
		}

		@Override
		public void destroy() throws Exception {
			if (directory != null && Files.isDirectory(directory)) {
				log.info("Deleting sample data directory {}", directory);
				FileSystemUtils.deleteRecursively(directory);
			}
		}

	}
}

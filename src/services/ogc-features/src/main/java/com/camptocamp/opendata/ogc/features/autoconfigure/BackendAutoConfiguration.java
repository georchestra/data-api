package com.camptocamp.opendata.ogc.features.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.camptocamp.opendata.ogc.features.reader.IndexedReader;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreRepository;
import com.camptocamp.opendata.producer.DatasetReader;
import com.camptocamp.opendata.producer.Producers;

@AutoConfiguration
public class BackendAutoConfiguration {

	@Bean
	CollectionRepository collectionRepository(Producers producers) {
		return new DataStoreRepository(producers);
	}

	@Bean
	Producers producers(List<DatasetReader> readers) {
		return new Producers(readers);
	}

	@Bean
	@Profile("!sample-data")
	@ConditionalOnMissingBean
	IndexedReader defaultIndexedReader() {
		throw new UnsupportedOperationException();
	}

}

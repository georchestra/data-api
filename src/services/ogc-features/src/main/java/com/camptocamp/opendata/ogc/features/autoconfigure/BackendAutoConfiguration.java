package com.camptocamp.opendata.ogc.features.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.ogc.features.reader.IndexedReader;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreRepository;
import com.camptocamp.opendata.producer.DatasetReader;
import com.camptocamp.opendata.producer.Producers;
import com.camptocamp.opendata.producer.geotools.GeoToolsDataReader;

import lombok.NonNull;

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
    @Profile("sample-data")
    IndexedReader sampleDataIndexedReader(@NonNull GeoToolsDataReader gtReader) {
        return new IndexedReader(gtReader) {

            @Override
            protected DataQuery toGtQuery(@NonNull DataQuery query) {
                // TODO convert the URI to a CSV URI hitting the layer name.csv in the classpath
                return null;
            }
        };
    }

    @Bean
    @Profile("!sample-data")
    IndexedReader defaultIndexedReader() {
        throw new UnsupportedOperationException();
    }

}

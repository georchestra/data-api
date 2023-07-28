package com.camptocamp.opendata.indexing.producer;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.camptocamp.opendata.producer.DatasetReader;
import com.camptocamp.opendata.producer.Producers;

@AutoConfiguration
public class ProducersAutoConfiguration {

    @Bean
    Producers producers(List<DatasetReader> readers) {
        return new Producers(readers);
    }
}

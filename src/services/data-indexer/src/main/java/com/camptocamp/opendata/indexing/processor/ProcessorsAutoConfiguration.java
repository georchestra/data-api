package com.camptocamp.opendata.indexing.processor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.camptocamp.opendata.processor.StandardProcessors;
import com.camptocamp.opendata.processor.geotools.GeoToolsProcessors;

@AutoConfiguration
public class ProcessorsAutoConfiguration {

    @Bean
    Processors processors() {
        return new Processors();
    }

    @Bean
    StandardProcessors standardProcessors() {
        return new StandardProcessors();
    }

    @Bean
    GeoToolsProcessors geoToolsProcessors() {
        return new GeoToolsProcessors();
    }
}

package com.camptocamp.opendata.indexing.processor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.camptocamp.opendata.processor.StandardProcessors;
import com.camptocamp.opendata.processor.geotools.GeoToolsProcessors;

@AutoConfiguration
public class ProcessorsAutoConfiguration {

    public @Bean Processors processors() {
        return new Processors();
    }

    public @Bean StandardProcessors standardProcessors() {
        return new StandardProcessors();
    }

    public @Bean GeoToolsProcessors geoToolsProcessors() {
        return new GeoToolsProcessors();
    }
}

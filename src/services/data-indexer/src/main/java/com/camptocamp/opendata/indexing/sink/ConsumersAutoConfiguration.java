package com.camptocamp.opendata.indexing.sink;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ConsumersAutoConfiguration {

    @Bean
    Consumers consumers() {
        return new Consumers();
    }
}

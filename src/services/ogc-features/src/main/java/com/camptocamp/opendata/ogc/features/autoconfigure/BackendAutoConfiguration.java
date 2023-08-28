package com.camptocamp.opendata.ogc.features.autoconfigure;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.camptocamp.opendata.ogc.features.http.codec.csv.CsvFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.shp.ShapefileFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.xls.Excel2007FeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreRepository;
import com.camptocamp.opendata.producer.geotools.FeatureToRecord;

@AutoConfiguration
public class BackendAutoConfiguration implements WebMvcConfigurer {

    @Bean
    CollectionRepository collectionRepository(@Qualifier("indexDataStore") DataStore indexStore) {
        return new DataStoreRepository(indexStore, new FeatureToRecord());
    }

    @Bean
    @Profile("!sample-data")
    DataStore indexDataStore(Map<String, Object> connectionProperties) throws IOException {
        return DataStoreFinder.getDataStore(connectionProperties);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new CsvFeatureCollectionHttpMessageConverter());
        converters.add(new ShapefileFeatureCollectionHttpMessageConverter());
        converters.add(new Excel2007FeatureCollectionHttpMessageConverter());
    }
}

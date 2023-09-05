package com.camptocamp.opendata.ogc.features.autoconfigure;

import com.camptocamp.opendata.ogc.features.http.codec.csv.CsvFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.shp.ShapefileFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.xls.Excel2007FeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.repository.DataStoreRepository;
import com.camptocamp.opendata.producer.geotools.FeatureToRecord;
import org.geotools.data.DataStore;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.support.DatabaseStartupValidator;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@AutoConfiguration
public class BackendAutoConfiguration implements WebMvcConfigurer {

    @Bean
    CollectionRepository collectionRepository(@Qualifier("indexDataStore") DataStore indexStore) {
        return new DataStoreRepository(indexStore, new FeatureToRecord());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new CsvFeatureCollectionHttpMessageConverter());
        converters.add(new ShapefileFeatureCollectionHttpMessageConverter());
        converters.add(new Excel2007FeatureCollectionHttpMessageConverter());
    }

    @Bean(name = "indexDataStore")
    @DependsOn("databaseStartupValidator")
    @Profile("postgis")
    DataStore postgisDataStore(DataSource dataSource, @Value("${pg.schema:opendataindex}") String schema)
            throws IOException {
        Map<String, ?> params = Map.of(//
                PostgisNGDataStoreFactory.DBTYPE.key, "postgis", //
                PostgisNGDataStoreFactory.DATASOURCE.key, dataSource, //
                PostgisNGDataStoreFactory.SCHEMA.key, schema, //
                PostgisNGDataStoreFactory.PREPARED_STATEMENTS.key, true, //
                PostgisNGDataStoreFactory.ENCODE_FUNCTIONS.key, true, //
                PostgisNGDataStoreFactory.ESTIMATED_EXTENTS.key, true, //
                PostgisNGDataStoreFactory.LOOSEBBOX.key, true//
        );

        PostgisNGDataStoreFactory fac = new PostgisNGDataStoreFactory();
        return fac.createDataStore(params);
    }

    @Bean
    @Profile("postgis")
    DatabaseStartupValidator databaseStartupValidator(DataSource dataSource) {
        var dsv = new DatabaseStartupValidator();
        dsv.setDataSource(dataSource);
        return dsv;
    }
}

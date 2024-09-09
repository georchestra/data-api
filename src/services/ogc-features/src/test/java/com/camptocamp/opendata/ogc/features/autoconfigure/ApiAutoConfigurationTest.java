package com.camptocamp.opendata.ogc.features.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import com.camptocamp.opendata.ogc.features.autoconfigure.api.ApiAutoConfiguration;
import com.camptocamp.opendata.ogc.features.autoconfigure.geotools.SampleDataBackendAutoConfiguration;
import com.camptocamp.opendata.ogc.features.autoconfigure.postgis.PostgisBackendAutoConfiguration;
import com.camptocamp.opendata.ogc.features.http.codec.csv.CsvFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.shp.ShapefileFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.xls.Excel2007FeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.server.api.CapabilitiesApiController;
import com.camptocamp.opendata.ogc.features.server.api.CapabilitiesApiDelegate;
import com.camptocamp.opendata.ogc.features.server.api.DataApiController;
import com.camptocamp.opendata.ogc.features.server.api.DataApiDelegate;
import com.camptocamp.opendata.ogc.features.server.config.HomeController;

class ApiAutoConfigurationTest {

    private WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withPropertyValues("spring.profiles.active=sample-data")
            .withConfiguration(AutoConfigurations.of(SampleDataBackendAutoConfiguration.class,
                    PostgisBackendAutoConfiguration.class, ApiAutoConfiguration.class));

    @Test
    void testExpectedBeans() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(HomeController.class)
                    .hasSingleBean(CapabilitiesApiController.class).hasSingleBean(CapabilitiesApiDelegate.class)
                    .hasSingleBean(DataApiController.class).hasSingleBean(DataApiDelegate.class)
                    .hasSingleBean(Excel2007FeatureCollectionHttpMessageConverter.class)
                    .hasSingleBean(ShapefileFeatureCollectionHttpMessageConverter.class)
                    .hasSingleBean(CsvFeatureCollectionHttpMessageConverter.class);
        });
    }

}

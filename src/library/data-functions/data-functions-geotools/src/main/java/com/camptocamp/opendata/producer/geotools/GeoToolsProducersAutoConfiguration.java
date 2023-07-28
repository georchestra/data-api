package com.camptocamp.opendata.producer.geotools;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties
public class GeoToolsProducersAutoConfiguration {

    @Bean
    GeoToolsDataReader geoToolsDataReader(List<GeoToolsFormat> supportedFormats) {
        GeoToolsDataReader geoToolsDataReader = new GeoToolsDataReader();
        geoToolsDataReader.setSupportedFormats(supportedFormats);
        return geoToolsDataReader;
    }

    @ConfigurationProperties(prefix = "producers.geotools.shp")
    @Bean
    ShapefileFormatProperties geotoolsShapefileFormatProperties() {
        return new ShapefileFormatProperties();
    }

    @Bean
    ShapefileFormat geotoolsShapefileFormat() {
        ShapefileFormat shapefileFormat = new ShapefileFormat();
        shapefileFormat.setDefaults(geotoolsShapefileFormatProperties());
        return shapefileFormat;
    }

    @Bean
    CsvFileFormat geotoolsCsvFileFormat() {
        return new CsvFileFormat();
    }

    @ConfigurationProperties(prefix = "producers.geotools.wfs")
    @Bean
    WfsFormatProperties geotoolsWfsFormatProperties() {
        return new WfsFormatProperties();
    }

    @Bean
    WfsFormat geotoolsWfsFormat() {
        WfsFormat wfsFormat = new WfsFormat();
        wfsFormat.setDefaults(geotoolsWfsFormatProperties());
        return wfsFormat;
    }

    // Disabled, results in a java.lang.StackOverflowError at current 25.x
    // GeoTools version
    // @Bean GeoJsonFormat geotoolsGeoJsonFormat() {
    // return new GeoJsonFormat();
    // }

    @ConfigurationProperties(prefix = "producers.geotools.geopackage")
    @Bean
    GeoPackageFormatProperties geotoolsGeoPackageFormatProperties() {
        return new GeoPackageFormatProperties();
    }

    @Bean
    GeoPackageFormat geotoolsGeoPackageFormat() {
        GeoPackageFormat geoPackageFormat = new GeoPackageFormat();
        geoPackageFormat.setDefaults(geotoolsGeoPackageFormatProperties());
        return geoPackageFormat;
    }
}

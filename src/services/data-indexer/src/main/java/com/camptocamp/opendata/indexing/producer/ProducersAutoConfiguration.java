package com.camptocamp.opendata.indexing.producer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.camptocamp.opendata.producer.DatasetReader;
import com.camptocamp.opendata.producer.Producers;
import com.camptocamp.opendata.producer.geotools.GeoPackageFormat;
import com.camptocamp.opendata.producer.geotools.GeoPackageFormatProperties;
import com.camptocamp.opendata.producer.geotools.GeoToolsDataReader;
import com.camptocamp.opendata.producer.geotools.GeoToolsFormat;
import com.camptocamp.opendata.producer.geotools.ShapefileFormat;
import com.camptocamp.opendata.producer.geotools.ShapefileFormatProperties;
import com.camptocamp.opendata.producer.geotools.WfsFormat;
import com.camptocamp.opendata.producer.geotools.WfsFormatProperties;

@AutoConfiguration
public class ProducersAutoConfiguration {

    public @Bean Producers producers(List<DatasetReader> readers) {
        return new Producers(readers);
    }

    public @Bean GeoToolsDataReader geoToolsDataReader(List<GeoToolsFormat> supportedFormats) {
        GeoToolsDataReader geoToolsDataReader = new GeoToolsDataReader();
        geoToolsDataReader.setSupportedFormats(supportedFormats);
        return geoToolsDataReader;
    }

    @ConfigurationProperties(prefix = "dataviz.indexer.producers.shp")
    public @Bean ShapefileFormatProperties geotoolsShapefileFormatProperties() {
        return new ShapefileFormatProperties();
    }

    public @Bean ShapefileFormat geotoolsShapefileFormat() {
        ShapefileFormat shapefileFormat = new ShapefileFormat();
        shapefileFormat.setDefaults(geotoolsShapefileFormatProperties());
        return shapefileFormat;
    }

    @ConfigurationProperties(prefix = "dataviz.indexer.producers.wfs")
    public @Bean WfsFormatProperties geotoolsWfsFormatProperties() {
        return new WfsFormatProperties();
    }

    public @Bean WfsFormat geotoolsWfsFormat() {
        WfsFormat wfsFormat = new WfsFormat();
        wfsFormat.setDefaults(geotoolsWfsFormatProperties());
        return wfsFormat;
    }

    // Disabled, results in a java.lang.StackOverflowError at current 25.x
    // GeoTools version
    // public @Bean GeoJsonFormat geotoolsGeoJsonFormat() {
    // return new GeoJsonFormat();
    // }

    @ConfigurationProperties(prefix = "dataviz.indexer.producers.geopackage")
    public @Bean GeoPackageFormatProperties geotoolsGeoPackageFormatProperties() {
        return new GeoPackageFormatProperties();
    }

    public @Bean GeoPackageFormat geotoolsGeoPackageFormat() {
        GeoPackageFormat geoPackageFormat = new GeoPackageFormat();
        geoPackageFormat.setDefaults(geotoolsGeoPackageFormatProperties());
        return geoPackageFormat;
    }
}

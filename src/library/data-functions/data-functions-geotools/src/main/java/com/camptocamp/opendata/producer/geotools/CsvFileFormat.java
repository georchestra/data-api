package com.camptocamp.opendata.producer.geotools;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.geotools.data.csv.CSVDataStoreFactory;

import com.camptocamp.opendata.model.DataQuery;

import lombok.NonNull;

class CsvFileFormat extends GeoToolsFormat {

    private static final CSVDataStoreFactory FACTORY = new CSVDataStoreFactory();

    @Override
    public String getName() {
        return FACTORY.getDisplayName();
    }

    @Override
    public boolean canHandle(@NonNull URI datasetUri) {
        return FACTORY.canProcess(super.toURL(datasetUri));
    }

    @Override
    protected Map<String, ?> toConnectionParams(@NonNull DataQuery query) {
        URL url = super.toURL(query.getSource().getUri());
        return Map.of(CSVDataStoreFactory.URL_PARAM.key, url);
    }

}

package com.camptocamp.opendata.producer.geotools;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.geotools.data.geojson.store.GeoJSONDataStoreFactory;

import com.camptocamp.opendata.model.DataQuery;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GeoJsonFormat extends GeoToolsFormat {

    private static final GeoJSONDataStoreFactory FACTORY = new GeoJSONDataStoreFactory();

    private final @Getter String name = "GeoJSON";

    @Override
    public boolean canHandle(@NonNull URI datasetUri) {
        try {
            return FACTORY.canProcess(toURL(datasetUri));
        } catch (RuntimeException e) {
            log.info("Error getting URL from URI", e);
            return false;
        }
    }

    @Override
    protected Map<String, ?> toConnectionParams(@NonNull DataQuery query) {
        final URI datasetUri = query.getSource().getUri();
        Objects.requireNonNull(datasetUri, "DataQuery.uri is required");
        URL url = toURL(datasetUri);
        Map<String, Object> params = new HashMap<>();
        params.put(GeoJSONDataStoreFactory.URL_PARAM.key, url);
        return params;
    }
}

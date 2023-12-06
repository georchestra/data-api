package com.camptocamp.opendata.producer.geotools;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.producer.DatasetReader;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@Slf4j(topic = "com.camptocamp.opendata.producer.geotools")
public class GeoToolsDataReader implements DatasetReader {

    private @Getter @Setter List<GeoToolsFormat> supportedFormats = new ArrayList<>();

    @Override
    public boolean canHandle(URI datasetUri) {
        return supportedFormats.stream().anyMatch(f -> f.canHandle(datasetUri));
    }

    @Override
    public Stream<GeodataRecord> read(@NonNull DataQuery query) {
        final GeoToolsFormat formatReader = findReader(query);
        return formatReader.read(query);
    }

    @Override
    public Long count(@NonNull DataQuery query) {
        final GeoToolsFormat formatReader = findReader(query);
        return formatReader.count(query);
    }

    private GeoToolsFormat findReader(DataQuery query) {
        final URI datasetUri = query.getSource().getUri();
        final GeoToolsFormat formatReader = findReader(datasetUri);
        return formatReader;
    }

    private GeoToolsFormat findReader(final URI datasetUri) {
        log.debug("Looking up a format reader for {}", datasetUri);
        final GeoToolsFormat formatReader = supportedFormats.stream().filter(f -> f.canHandle(datasetUri)).findFirst()
                .orElseThrow(IllegalArgumentException::new);
        log.debug("Found format reader {} for {}", formatReader.getClass().getSimpleName(), datasetUri);
        return formatReader;
    }

    @Override
    public String getName() {
        return String.format("GeoTools[%s]",
                supportedFormats.stream().map(GeoToolsFormat::getName).collect(Collectors.joining(",")));
    }

}

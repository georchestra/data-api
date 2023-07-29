package com.camptocamp.opendata.ogc.features.reader;

import java.net.URI;
import java.util.stream.Stream;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.producer.DatasetReader;
import com.camptocamp.opendata.producer.geotools.GeoToolsDataReader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class IndexedReader implements DatasetReader {

    private final @NonNull GeoToolsDataReader gtReader;

    @Override
    public String getName() {
        return "IndexedReader";
    }

    @Override
    public boolean canHandle(URI datasetUri) {
        return "index".equals(datasetUri.getScheme());
    }

    @Override
    public Stream<GeodataRecord> read(@NonNull DataQuery query) {
        DataQuery gtQuery = toGtQuery(query);
        return gtReader.read(gtQuery);
    }

    @Override
    public Long count(@NonNull DataQuery query) {
        DataQuery gtQuery = toGtQuery(query);
        return gtReader.count(gtQuery);
    }

    protected abstract DataQuery toGtQuery(@NonNull DataQuery query);

}

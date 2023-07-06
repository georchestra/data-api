package com.camptocamp.opendata.producer;

import java.net.URI;
import java.util.stream.Stream;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;

import lombok.NonNull;

public interface DatasetReader {

    String getName();

    boolean canHandle(URI datasetUri);

    Stream<GeodataRecord> read(@NonNull DataQuery query);

}

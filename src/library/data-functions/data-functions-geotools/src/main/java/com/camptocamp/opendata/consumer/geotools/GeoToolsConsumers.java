package com.camptocamp.opendata.consumer.geotools;

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.camptocamp.opendata.model.DataSource;
import com.camptocamp.opendata.model.GeodataRecord;

import lombok.NonNull;

public class GeoToolsConsumers {

    public Consumer<Stream<GeodataRecord>> importTo(DataSource target) {
        throw new UnsupportedOperationException();
    }

    public void index(@NonNull Stream<GeodataRecord> records) {
        throw new UnsupportedOperationException();
    }
}

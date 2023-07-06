package com.camptocamp.opendata.processor;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.model.GeometryProperty;
import com.camptocamp.opendata.model.SimpleProperty;

import lombok.NonNull;

public class StandardProcessors {

    public Function<Stream<GeodataRecord>, Stream<GeodataRecord>> renameProperty(//
            @NonNull String propertyName, //
            @NonNull String newPropertyname) {

        return stream -> stream.map(renameRecordProperty(propertyName, newPropertyname));
    }

    public Function<GeodataRecord, GeodataRecord> renameRecordProperty(//
            @NonNull String propertyName, //
            @NonNull String newPropertyname) {

        return record -> rename(record, propertyName, newPropertyname);
    }

    private GeodataRecord rename(GeodataRecord r, @NonNull String propertyName, //
            @NonNull String newPropertyname) {

        Optional<? extends SimpleProperty<?>> property = r.getProperty(propertyName);
        Optional<? extends SimpleProperty<?>> renamed = property.map(p -> p.withName(newPropertyname));

        if (renamed.isPresent()) {
            SimpleProperty<?> prop = renamed.get();
            if (prop instanceof GeometryProperty) {
                return r.withGeometry((GeometryProperty) prop);
            }
            return r.withProperty(prop);
        }
        return r;
    }
}

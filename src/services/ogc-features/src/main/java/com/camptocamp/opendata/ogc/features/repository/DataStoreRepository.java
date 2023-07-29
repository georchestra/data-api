package com.camptocamp.opendata.ogc.features.repository;

import java.time.OffsetDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.producer.Producers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataStoreRepository implements CollectionRepository {

    private final @NonNull Producers producers;

    @Override
    public FeatureCollection query(@NonNull DataQuery query) {
        Supplier<Stream<? extends GeodataRecord>> dataSupplier = producers.read(query);
        Long matched = producers.count(query.withLimit(null)).get();
        Long returned = producers.count(query).get();

        FeatureCollection fc = new FeatureCollection();
        fc.setNumberMatched(matched);
        fc.setNumberReturned(returned);
        fc.setFeatures(dataSupplier.get().toList());
        fc.setTimeStamp(OffsetDateTime.now());
        return fc;
    }
}

package com.camptocamp.ogc.features.repository;

import java.time.OffsetDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.camptocamp.ogc.features.server.model.FeatureCollection;
import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.producer.Producers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
public class DataStoreRepository implements CollectionRepository {

    private final @NonNull Producers producers;

    @Override
    public FeatureCollection query(@NonNull DataQuery query) {
        Supplier<Stream<? extends GeodataRecord>> dataSupplier = producers.read(query);
        FeatureCollection fc = new FeatureCollection();
        fc.setFeatures(() -> toFlux(dataSupplier));
        fc.setTimeStamp(OffsetDateTime.now());
        return fc;
    }

    /**
     * Adapts a {@link Stream} to a {@link Flux}, note that
     * {@link Flux#fromStream(Stream)} already makes sure {@link Stream#close()} is
     * called when the stream is fully consumed.
     */
    private <T> Flux<T> toFlux(Supplier<Stream<? extends T>> stream) {
        return Flux.fromStream(stream)//
                // subscribe on Schedulers.boundedElastic() to avoid
                // "java.lang.IllegalStateException: Iterating over a toIterable() / toStream()
                // is blocking, which is not supported in thread reactor-http-epoll-XXX"
                .subscribeOn(Schedulers.boundedElastic());
    }
}

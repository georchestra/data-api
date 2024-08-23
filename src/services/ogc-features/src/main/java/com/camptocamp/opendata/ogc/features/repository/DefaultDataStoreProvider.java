package com.camptocamp.opendata.ogc.features.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultDataStoreProvider extends AbstractDataStoreProvider {

    protected final @NonNull Map<String, Object> connectionParams;

    @Override
    protected @NonNull DataStore create() {
        try {
            return Objects.requireNonNull(DataStoreFinder.getDataStore(connectionParams),
                    "Unable to find datastore with the provided connection parameters");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

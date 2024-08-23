package com.camptocamp.geotools.data.decorate;

import org.geotools.api.data.DataStore;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Abstract {@link DataStore} decorator forwarding all method calls to the
 * decorated object
 */
@RequiredArgsConstructor
public abstract class DecoratingDataStore implements DataStore {

    @Delegate
    protected final @NonNull DataStore delegate;
}

package com.camptocamp.opendata.ogc.features.repository;

import java.util.function.Supplier;

import org.geotools.api.data.DataStore;

public interface DataStoreProvider extends Supplier<DataStore> {

    @Override
    DataStore get();

    public void reInit();
}
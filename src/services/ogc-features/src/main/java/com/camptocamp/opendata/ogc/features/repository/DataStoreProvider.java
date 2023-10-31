package com.camptocamp.opendata.ogc.features.repository;

import org.geotools.api.data.DataStore;

public interface DataStoreProvider {

    DataStore get();

    public void reInit();
}
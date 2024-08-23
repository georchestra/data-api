package com.camptocamp.opendata.ogc.features.repository;

import java.util.List;
import java.util.Optional;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;

public interface CollectionRepository {

    Optional<FeatureCollection> query(DataQuery query);

    List<Collection> getCollections();

    Optional<Collection> findCollection(String collectionId);

    /**
     * @param collectionId
     * @param featureId
     * @return
     */
    Optional<GeodataRecord> getRecord(String collectionId, String featureId);
}

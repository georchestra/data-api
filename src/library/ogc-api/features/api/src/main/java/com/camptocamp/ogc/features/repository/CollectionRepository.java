package com.camptocamp.ogc.features.repository;

import com.camptocamp.ogc.features.server.model.FeatureCollection;
import com.camptocamp.opendata.model.DataQuery;

public interface CollectionRepository {

    FeatureCollection query(DataQuery query);

}

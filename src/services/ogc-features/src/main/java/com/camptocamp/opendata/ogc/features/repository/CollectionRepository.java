package com.camptocamp.opendata.ogc.features.repository;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;

public interface CollectionRepository {

    FeatureCollection query(DataQuery query);

}

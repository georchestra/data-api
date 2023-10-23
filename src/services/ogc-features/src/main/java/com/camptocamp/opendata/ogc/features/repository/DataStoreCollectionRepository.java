package com.camptocamp.opendata.ogc.features.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.CRS;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.GeoToolsFeatureCollection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataStoreCollectionRepository implements CollectionRepository {

    // private final @NonNull Producers producers;
    private final @NonNull DataStore dataStore;
    private final @NonNull Function<SimpleFeature, GeodataRecord> featureMapper;

    private final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    @Override
    public List<Collection> getCollections() {
        try {
            String[] typeNames = dataStore.getTypeNames();
            return Arrays.stream(typeNames).map(this::loadCollection).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<Collection> findCollection(String collectionId) {
        try {
            return Optional.of(loadCollection(collectionId));
        } catch (UncheckedIOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<GeodataRecord> getRecord(String collectionId, String featureId) {
        SimpleFeature feature;

        Query query = new Query(collectionId, ff.id(ff.featureId(featureId)));
        SimpleFeatureCollection features = query(query);
        try (SimpleFeatureIterator it = features.features()) {
            feature = it.hasNext() ? it.next() : null;
        }

        return Optional.ofNullable(feature).map(featureMapper);
    }

    @Override
    public FeatureCollection query(@NonNull DataQuery query) {
        try {
            Collection collection = findCollection(query.getLayerName()).orElseThrow();
            Query gtQuery = toQuery(query);
            SimpleFeatureCollection features = query(gtQuery);

            long matched = count(toQuery(query.withLimit(null)));
            long returned = count(gtQuery);
            GeoToolsFeatureCollection ret = new GeoToolsFeatureCollection(collection, features);
            ret.setNumberMatched(matched);
            ret.setNumberReturned(returned);
            return ret;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private int count(Query query) {
        try {
            return dataStore.getFeatureSource(query.getTypeName()).getCount(query);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Query toQuery(@NonNull DataQuery query) {
        Query q = new Query(query.getLayerName());
        if (null != query.getLimit())
            q.setMaxFeatures(query.getLimit());
        if (null != query.getOffset())
            q.setStartIndex(query.getOffset());
        if (null != query.getFilter()) {
            try {
                q.setFilter(CQL.toFilter(query.getFilter()));
            } catch (CQLException e) {
                throw new RuntimeException(e);
            }
        }
        return q;
    }

    private SimpleFeatureCollection query(Query query) {
        try {
            String typeName = query.getTypeName();
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            return featureSource.getFeatures(query);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Collection loadCollection(String typeName) {
        Collection c = new Collection(typeName, new ArrayList<>());
        c.setTitle(typeName);
        SimpleFeatureType schema;
        try {
            schema = dataStore.getSchema(typeName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        GeometryDescriptor geom = schema.getGeometryDescriptor();
        if (geom == null) {
            c.setCrs(List.of());
            c.setItemType("record");
        } else {
            c.setItemType("feature");
            CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
            if (null != crs) {
                String srs = CRS.toSRS(crs);
                c.addCrsItem(srs);
            }
        }
        return c;
    }
}

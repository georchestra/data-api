package com.camptocamp.opendata.ogc.features.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.GeoToolsFeatureCollection;
import com.camptocamp.opendata.producer.Producers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataStoreRepository implements CollectionRepository {

    // private final @NonNull Producers producers;
    private final @NonNull DataStore dataStore;
    private final @NonNull Function<SimpleFeature, GeodataRecord> featureMapper;

    private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

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
        Query gtQuery = toQuery(query);
        SimpleFeatureCollection features = query(gtQuery);

        long matched = count(toQuery(query.withLimit(null)));
        long returned = count(gtQuery);
        GeoToolsFeatureCollection ret = new GeoToolsFeatureCollection(features);
        ret.setNumberMatched(matched);
        ret.setNumberReturned(returned);
        return ret;
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
        } else {
            CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
            String srs = CRS.toSRS(crs);
            c.addCrsItem(srs);
        }
        return c;
    }
}

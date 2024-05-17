package com.camptocamp.opendata.ogc.features.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.CRS;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.GeoToolsFeatureCollection;
import com.google.common.base.Throwables;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j(topic = "com.camptocamp.opendata.ogc.features.repository")
public class DataStoreCollectionRepository implements CollectionRepository {

    private final @NonNull DataStoreProvider dataStoreProvider;
    private final @NonNull Function<SimpleFeature, GeodataRecord> featureMapper;

    private final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    private DataStore dataStore() {
        return dataStoreProvider.get();
    }

    @Override
    public List<Collection> getCollections() {
        try {
            String[] typeNames = dataStore().getTypeNames();
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

        final Query query = new Query(collectionId, ff.id(ff.featureId(featureId)));

        return runWithRetry("getRecord(%s:%s)".formatted(collectionId, featureId), () -> {
            SimpleFeatureCollection features = query(query);
            SimpleFeature feature;
            try (SimpleFeatureIterator it = features.features()) {
                feature = it.hasNext() ? it.next() : null;
            }

            return Optional.ofNullable(feature).map(featureMapper);
        });
    }

    private <T> T runWithRetry(String description, Callable<T> command) {
        try {
            return command.call();
        } catch (Exception e) {
            String message = Throwables.getRootCause(e).getMessage();
            if (log.isDebugEnabled()) {
                log.info("Retrying command %s: %s".formatted(description, message), e);
            } else {
                log.info("Retrying command %s: %s".formatted(description, message));
            }
            dataStoreProvider.reInit();
            try {
                return command.call();
            } catch (Exception e2) {
                log.info("Retry command failed. Giving up for %s".formatted(description));
                if (e2 instanceof RuntimeException) {
                    throw (RuntimeException) e2;
                }
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public FeatureCollection query(@NonNull DataQuery query) {
        final Collection collection = findCollection(query.getLayerName()).orElseThrow();
        final Query gtQuery = toQuery(query);
        return runWithRetry("query(%s)".formatted(query.getLayerName()), () -> {
            ensureSchemaIsInSync(gtQuery);
            SimpleFeatureCollection fc = query(gtQuery);
            long matched = count(toQuery(query.withLimit(null).withOffset(null)));
            long returned = count(gtQuery);
            GeoToolsFeatureCollection ret = new GeoToolsFeatureCollection(collection, fc);
            ret.setNumberMatched(matched);
            ret.setNumberReturned(returned);
            ret.setTargetCrs(query.getTargetCrs());
            return ret;
        });
    }

    /**
     * Workaround to make sure the datastore cached featuretype is in sync with the
     * one in the database in case it has changed under the hood
     */
    private void ensureSchemaIsInSync(Query gtQuery) {
        Query noopQuery = new Query(gtQuery);
        noopQuery.setMaxFeatures(0);
        SimpleFeatureCollection fc = query(noopQuery);
        try (SimpleFeatureIterator it = fc.features()) {

        } catch (RuntimeException e) {
            throw e;
        }
    }

    private int count(Query query) {
        try {
            return dataStore().getFeatureSource(query.getTypeName()).getCount(query);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Query toQuery(@NonNull DataQuery query) {
        Query q = new Query(query.getLayerName());
        Integer limit = query.getLimit();
        Integer offset = query.getOffset();

        if (null != limit)
            q.setMaxFeatures(query.getLimit());
        if (null != offset)
            q.setStartIndex(query.getOffset());
        if (null != query.getFilter()) {
            try {
                q.setFilter(ECQL.toFilter(query.getFilter()));
            } catch (CQLException e) {
                throw new RuntimeException(e);
            }
        }
        List<SortBy> sortBy = sortBy(query);
        if (null != limit || null != offset) {
            // always add natural order for paging consistency
            sortBy.add(SortBy.NATURAL_ORDER);
        }
        q.setSortBy(sortBy.toArray(SortBy[]::new));
        return q;
    }

    private List<SortBy> sortBy(DataQuery q) {
        return q.getSortBy().stream().map(this::toSortBy).collect(Collectors.toList());
    }

    private SortBy toSortBy(DataQuery.SortBy order) {
        return ff.sort(order.propertyName(), order.ascending() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
    }

    private SimpleFeatureCollection query(Query query) {
        try {
            String typeName = query.getTypeName();
            SimpleFeatureSource featureSource = dataStore().getFeatureSource(typeName);
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
            schema = dataStore().getSchema(typeName);
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

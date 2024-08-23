package com.camptocamp.geotools.data.decorate;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.geotools.api.data.DataAccess;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;

import lombok.NonNull;

class RenamingFeatureSource extends DecoratingFeatureSource implements SimpleFeatureSource {

    private final String typeName;
    private final RenamingDataStore renamingStore;

    private SimpleFeatureType schema;

    public RenamingFeatureSource(@NonNull String typeName, @NonNull SimpleFeatureSource delegate,
            @NonNull RenamingDataStore renamingStore) {
        super(delegate);
        this.typeName = typeName;
        this.renamingStore = renamingStore;
    }

    @Override
    public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
        return renamingStore;
    }

    @Override
    public SimpleFeatureType getSchema() {
        if (null == schema) {
            try {
                schema = renamingStore.getSchema(typeName);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return schema;
    }

    @Override
    public Name getName() {
        return getSchema().getName();
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return delegate.getBounds(toSourceQuery(query));
    }

    @Override
    public int getCount(Query query) throws IOException {
        return delegate.getCount(toSourceQuery(query));
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return new ReTypingFeatureCollection(delegate.getFeatures(), getSchema());
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return new ReTypingFeatureCollection(delegate.getFeatures(filter), getSchema());
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        return new ReTypingFeatureCollection(delegate.getFeatures(toSourceQuery(query)), getSchema());
    }

    Query toSourceQuery(Query query) {
        Query q = new Query(query);
        q.setTypeName(delegate.getName().getLocalPart());
        return q;
    }

}

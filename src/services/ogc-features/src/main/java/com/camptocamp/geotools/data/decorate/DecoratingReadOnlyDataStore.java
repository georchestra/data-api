package com.camptocamp.geotools.data.decorate;

import static com.camptocamp.geotools.data.decorate.ReadOnlyDataStore.readOnlyException;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;

import lombok.NonNull;

/**
 * A read-only {@link DecoratingDataStore}, where all mutating methods throw an
 * {@link UnsupportedOperationException}
 */
public abstract class DecoratingReadOnlyDataStore extends DecoratingDataStore implements ReadOnlyDataStore {

    protected DecoratingReadOnlyDataStore(@NonNull DataStore delegate) {
        super(delegate);
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Filter filter,
            Transaction transaction) {
        throw readOnlyException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Transaction transaction) {
        throw readOnlyException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName,
            Transaction transaction) {
        throw readOnlyException();
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) {
        throw readOnlyException();
    }

    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) {
        throw readOnlyException();
    }

    @Override
    public void removeSchema(Name typeName) {
        throw readOnlyException();
    }

    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) {
        throw readOnlyException();
    }

    @Override
    public void removeSchema(String typeName) {
        throw readOnlyException();
    }
}

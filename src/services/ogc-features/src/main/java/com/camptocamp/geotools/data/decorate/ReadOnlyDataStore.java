package com.camptocamp.geotools.data.decorate;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.LockingManager;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.InProcessLockingManager;

/**
 * Read-only {@link DataStore} interface extension where all mutating methods
 * throw an {@link UnsupportedOperationException}
 */
public interface ReadOnlyDataStore extends DataStore {

    LockingManager IN_PROCESS_LOCKING = new InProcessLockingManager();

    @Override
    default LockingManager getLockingManager() {
        return IN_PROCESS_LOCKING;
    }

    @Override
    default FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Filter filter,
            Transaction transaction) {
        throw readOnlyException();
    }

    @Override
    default FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Transaction transaction) {
        throw readOnlyException();
    }

    @Override
    default FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName,
            Transaction transaction) {
        throw readOnlyException();
    }

    @Override
    default void createSchema(SimpleFeatureType featureType) {
        throw readOnlyException();
    }

    @Override
    default void updateSchema(Name typeName, SimpleFeatureType featureType) {
        throw readOnlyException();
    }

    @Override
    default void removeSchema(Name typeName) {
        throw readOnlyException();
    }

    @Override
    default void updateSchema(String typeName, SimpleFeatureType featureType) {
        throw readOnlyException();
    }

    @Override
    default void removeSchema(String typeName) {
        throw readOnlyException();
    }

    static UnsupportedOperationException readOnlyException() {
        return new UnsupportedOperationException("read only");
    }
}

package com.camptocamp.opendata.ogc.features.autoconfigure.postgis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.data.DefaultServiceInfo;

import com.camptocamp.geotools.data.decorate.ReadOnlyDataStore;

import lombok.RequiredArgsConstructor;

/**
 * A read-only {@link DataStore} that multiplexes to a number of others
 * {@link DataStore}s based on the PostgreSQL schema they're configured against.
 * <p>
 * The {@link SchemaMultiplexingPostgisDataStoreProvider} is in charge of
 * resolving the internal {@link DataStore} to use for each FeatureTypeName, by
 * either applying the schema as a prefix or removing it
 * 
 * @see SchemaMultiplexingPostgisDataStoreProvider
 */
@RequiredArgsConstructor
class SchemaMultiplexingPostgisDataStore implements ReadOnlyDataStore {

    private final SchemaMultiplexingPostgisDataStoreProvider provider;

    @Override
    public void dispose() {
        provider.disposeAll();
    }

    @Override
    public String[] getTypeNames() throws IOException {
        return getDataStores().map(this::getTypeNames).flatMap(Stream::of).sorted().toArray(String[]::new);
    }

    @Override
    public List<Name> getNames() throws IOException {
        return getDataStores().map(this::getNames).flatMap(List::stream)
                .sorted((n1, n2) -> n1.getLocalPart().compareTo(n2.getLocalPart())).toList();
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        return getDataStore(typeName).getSchema(typeName);
    }

    @Override
    public SimpleFeatureType getSchema(Name name) throws IOException {
        return getDataStore(name.getLocalPart()).getSchema(name);
    }

    @Override
    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        return getDataStore(typeName).getFeatureSource(typeName);
    }

    @Override
    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        return getDataStore(typeName.getLocalPart()).getFeatureSource(typeName);
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction)
            throws IOException {
        Objects.requireNonNull(query.getTypeName());
        return getDataStore(query.getTypeName()).getFeatureReader(query, transaction);
    }

    // this method is unused in this application
    @Override
    public ServiceInfo getInfo() {
        return new DefaultServiceInfo();
    }

    private Stream<DataStore> getDataStores() {
        return provider.getDataStores();
    }

    private DataStore getDataStore(String typeName) {
        return provider.getDataStore(provider.getSchema(typeName));
    }

    private List<Name> getNames(DataStore datastore) {
        try {
            return datastore.getNames();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String[] getTypeNames(DataStore datastore) {
        try {
            return datastore.getTypeNames();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
package com.camptocamp.geotools.data.decorate;

import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.data.ReTypeFeatureReader;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

import lombok.NonNull;

/**
 * Decorates a single {@link DataStore} to retype it's {@link FeatureTypes} by
 * applying a transformation to the type name
 */
public class RenamingDataStore extends DecoratingReadOnlyDataStore {

    private final UnaryOperator<String> rename;
    private final UnaryOperator<String> undoRename;

    /**
     * @param delegate   the DataStore to decorate
     * @param rename     function applied to FeatureType names before returning them
     * @param undoRename function to undo the rename when talking to the decorated
     *                   DataStore
     */
    public RenamingDataStore(@NonNull DataStore delegate, @NonNull UnaryOperator<String> rename,
            @NonNull UnaryOperator<String> undoRename) {
        super(delegate);
        this.rename = rename;
        this.undoRename = undoRename;
    }

    /**
     * {@inheritDoc}
     * <p>
     * All names are transformed using the {@code rename} function provided to the
     * constructor
     */
    @Override
    public String[] getTypeNames() throws IOException {
        return Stream.of(super.getTypeNames()).map(rename).toArray(String[]::new);
    }

    /**
     * {@inheritDoc}
     * <p>
     * All names are transformed using the {@code rename} function provided to the
     * constructor
     */
    @Override
    public List<Name> getNames() throws IOException {
        return super.getNames().stream().map(n -> new NameImpl(n.getNamespaceURI(), rename.apply(n.getLocalPart())))
                .map(Name.class::cast).toList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The FeatureType name is transformed using the {@code rename} function
     * provided to the constructor
     */
    @Override
    public SimpleFeatureType getSchema(Name name) throws IOException {
        return getSchema(name.getLocalPart());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The FeatureType name is transformed using the {@code rename} function
     * provided to the constructor
     */
    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        final String origTypeName = undoRename.apply(typeName);
        SimpleFeatureType featureType = super.getSchema(origTypeName);
        if (!typeName.equals(origTypeName)) {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.init(featureType);
            builder.setName(typeName);
            featureType = builder.buildFeatureType();
        }
        return featureType;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The {@link FeatureSource}'s {@link FeatureSource#getSchema() schema} and
     * {@link FeatureSource#getFeatures() feature collection} are transformed using
     * the {@code rename} function provided to the constructor, and hence so the
     * feature collection's features themselves
     */
    @Override
    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        String unprefixedTypeName = undoRename.apply(typeName);
        var featureSource = super.getFeatureSource(unprefixedTypeName);
        if (!typeName.equals(unprefixedTypeName)) {
            featureSource = new RenamingFeatureSource(typeName, featureSource, this);
        }
        return featureSource;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The {@link FeatureSource}'s {@link FeatureSource#getSchema() schema} and
     * {@link FeatureSource#getFeatures() feature collection} are transformed using
     * the {@code rename} function provided to the constructor, and hence so the
     * feature collection's features themselves
     */
    @Override
    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        return getFeatureSource(typeName.getLocalPart());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The FeatureType name is transformed using the {@code rename} function
     * provided to the constructor
     */
    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction)
            throws IOException {
        String typeName = query.getTypeName();
        String unprefixedTypeName = undoRename.apply(typeName);

        if (typeName.equals(unprefixedTypeName)) {
            return super.getFeatureReader(query, transaction);
        }
        Query unprefixedQuery = new Query(query);
        unprefixedQuery.setTypeName(unprefixedTypeName);
        var reader = super.getFeatureReader(unprefixedQuery, transaction);
        SimpleFeatureType prefixedSchema = getSchema(typeName);
        return new ReTypeFeatureReader(reader, prefixedSchema);
    }
}

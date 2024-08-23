package com.camptocamp.opendata.ogc.features.autoconfigure.postgis;

import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.geotools.api.data.DataStore;

import com.camptocamp.geotools.data.decorate.PrefixingDataStore;
import com.camptocamp.opendata.ogc.features.repository.AbstractDataStoreProvider;
import com.camptocamp.opendata.ogc.features.repository.DataStoreProvider;
import com.camptocamp.opendata.ogc.features.repository.JdbcDataStoreProvider;

import lombok.NonNull;

/**
 * {@link DataStoreProvider} that {@link #get() provides} a data store decorator
 * working against all the PostgreSQL schemas configured to be
 * {@link PostgisSchemasConfiguration#includeSchema() visible} to the
 * application.
 * 
 * @see PostgisSchemasConfiguration
 */
class SchemaMultiplexingPostgisDataStoreProvider extends AbstractDataStoreProvider implements JdbcDataStoreProvider {

    private final PostgisSchemasConfiguration config;

    /**
     * DataSource shared amongst all the concrete stores for each schema
     */
    private DataSource dataSource;

    private final SchemaProviders schemaProviders;

    public SchemaMultiplexingPostgisDataStoreProvider(PostgisSchemasConfiguration config, DataSource dataSource) {
        this.config = config;
        this.dataSource = dataSource;
        this.schemaProviders = new SchemaProviders(config);
    }

    /**
     * Returns a {@link DataStore} that multiplexes for individual DataStores, one
     * for each available Postgres schema, and applies FeatureType name prefixing
     * based on the schema name or the alias defined for it
     */
    @Override
    protected @NonNull DataStore create() {
        return new SchemaMultiplexingPostgisDataStore(this);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void setDataSource(DataSource ds) {
        this.dataSource = ds;
        schemaProviders.getProviders().forEach(p -> p.setDataSource(ds));
    }

    /**
     * @return the actual postgres schema from a prefixed type name, may require
     *         un-aliasing the type name prefix
     */
    public String getSchema(String schemaPrefixedTypeName) {
        String prefix = config.extractPrefix(schemaPrefixedTypeName);
        return config.unalias(prefix);
    }

    /**
     * @return the prefixing datastore for a given postgres schema, may alias the
     *         schema name for the returned datastore feature types
     */
    public PrefixingDataStore getDataStore(String postgresSchema) {
        DataStoreProvider plainStoreProvider = getProvider(postgresSchema);
        DataStore plainStore = plainStoreProvider.get();
        String prefix = config.prefix(postgresSchema);
        return new PrefixingDataStore(plainStore, () -> prefix);
    }

    /**
     * @return the plain (non-prefixing) datastore provider for a given postgres
     *         schema
     */
    public DataStoreProvider getProvider(@NonNull String postgresSchema) {
        return schemaProviders.getProvider(postgresSchema, dataSource);
    }

    public Stream<DataStore> getDataStores() {
        Set<String> schemaNames = getSchemas();
        return schemaNames.stream().map(this::getDataStore);
    }

    public Set<String> getSchemas() {
        return schemaProviders.getSchemas(dataSource);
    }

    /**
     * Call chain:
     * {@code super.destroy() -> SchemaMultiplexingPostgisDataStore.dispose() -> this.disposeAll()}
     */
    public void disposeAll() {
        schemaProviders.disposeAll();
    }
}
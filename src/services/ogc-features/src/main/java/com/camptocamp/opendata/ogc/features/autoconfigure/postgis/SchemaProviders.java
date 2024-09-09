package com.camptocamp.opendata.ogc.features.autoconfigure.postgis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.camptocamp.opendata.ogc.features.autoconfigure.postgis.PostgisSchemasConfiguration.Refresh;
import com.camptocamp.opendata.ogc.features.repository.DataStoreProvider;
import com.google.common.collect.Sets;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aids {@link SchemaMultiplexingPostgisDataStoreProvider} in obtaining a
 * {@link PostgisDataStoreProvider} for each visible postgres schema, and to
 * maintain the list of visible schemas up to date.
 * 
 * @see PostgisSchemasConfiguration
 */
@Slf4j
@RequiredArgsConstructor
class SchemaProviders {

    private final @NonNull PostgisSchemasConfiguration config;

    private final Lock schemaReloadLock = new ReentrantLock();
    private Instant cachedSchemasLastUpdated = Instant.ofEpochMilli(0);
    private Set<String> cachedSchemas;

    /**
     * Map of postgis datastore providers by PostgreSQL schema name (not alias)
     */
    private Map<String, PostgisDataStoreProvider> providersBySchema = new ConcurrentHashMap<>();

    public Set<String> getSchemas(DataSource dataSource) {

        if (!timeoutEnabled()) {
            Set<String> schemas = loadVisibleSchemas(dataSource);
            cleanupProviders(schemas);
            return schemas;
        }

        if (shallUpdateSchemaCache()) {
            schemaReloadLock.lock();
            try {
                if (shallUpdateSchemaCache()) {
                    log.info("reloading postgis schemas");
                    Set<String> schemas = loadVisibleSchemas(dataSource);
                    cleanupProviders(schemas);
                    cachedSchemas = schemas;
                    cachedSchemasLastUpdated = Instant.now();
                }
            } finally {
                schemaReloadLock.unlock();
            }
        }
        return cachedSchemas;
    }

    public List<PostgisDataStoreProvider> getProviders() {
        return List.copyOf(providersBySchema.values());
    }

    public DataStoreProvider getProvider(@NonNull String schema, @NonNull DataSource dataSource) {
        return providersBySchema.computeIfAbsent(schema, name -> createProvider(name, dataSource));
    }

    /**
     * Call chain:
     * {@code super.destroy() -> SchemaMultiplexingPostgisDataStore.dispose() -> this.disposeAll()}
     */
    public void disposeAll() {
        Set.copyOf(this.providersBySchema.keySet()).forEach(this::dispose);
        cachedSchemas = null;
    }

    public void dispose(String schema) {
        PostgisDataStoreProvider provider = this.providersBySchema.remove(schema);
        if (provider != null) {
            provider.destroy();
        }
    }

    private void cleanupProviders(Set<String> actualSchemas) {
        Set<String> usedSchemas = Set.copyOf(this.providersBySchema.keySet());
        Set<String> goneSchemas = Sets.difference(usedSchemas, actualSchemas);
        List<String> newSchemas = Sets.difference(actualSchemas, usedSchemas).stream().filter(config::includeSchema)
                .toList();

        if (!goneSchemas.isEmpty() || !newSchemas.isEmpty()) {
            log.info("gone schemas: {}, new schemas: {}", goneSchemas, newSchemas);

            goneSchemas.forEach(this::disposeRemovedSchema);
        }
    }

    private boolean shallUpdateSchemaCache() {
        return null == cachedSchemas || schemaCacheExpired();
    }

    private boolean schemaCacheExpired() {
        Instant expiresAt = cacheExpiringTime();
        return Instant.now().isAfter(expiresAt);
    }

    private Instant cacheExpiringTime() {
        Duration cacheTTL = refreshSettings().getInterval();
        return cachedSchemasLastUpdated.plus(cacheTTL);
    }

    private boolean timeoutEnabled() {
        return refreshSettings().isEnabled();
    }

    private Refresh refreshSettings() {
        Refresh refresh = config.getRefresh();
        return null == refresh ? new Refresh() : refresh;
    }

    private Set<String> loadVisibleSchemas(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final String query = "select schema_name from information_schema.schemata";
        try (Stream<String> stream = jdbcTemplate.queryForStream(query, (rs, rownum) -> rs.getString(1))) {
            return stream.filter(config::includeSchema).collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private void disposeRemovedSchema(String schema) {
        log.info("Disposing DataStore provider for removed schema {}", schema);
        dispose(schema);
    }

    private PostgisDataStoreProvider createProvider(String schema, DataSource dataSource) {
        return PostgisDataStoreProvider.newInstance(dataSource, schema);
    }

}

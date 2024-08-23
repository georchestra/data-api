package com.camptocamp.opendata.ogc.features.autoconfigure.postgis;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.google.common.annotations.VisibleForTesting;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Configuration properties to define how to handle multiple PostgreSQL schemas
 * to avoid FeatureType name clashes.
 * <p>
 * The default configuration is to serve data from all tables in all schemas,
 * except for a few hard-coded schema names: {@literal pg_toast},
 * {@literal pg_catalog}, {@literal information_schema}, {@literal topology},
 * and {@literal tiger}; and to prefix all feature type names with the schema
 * name and a {@code :} delimiter. For example, a table {@code locations} in the
 * {@code public} schema will be presented as {@code public:locations}.
 * <p>
 * Newly created database schemas, or removed schemas, will be noticed after a
 * period defined by the `postgis.schemas.refresh.interval` config property.
 * <p>
 * Default configuration:
 * 
 * <pre>
 * <code> 
 * postgis:
 *   schemas:
 *     delimiter: ":"
 *     refresh:
 *       enabled: true
 *       interval: PT5S
 *     include:
 *     - schema: "*"
 *       prefix-tables: true
 * </code>
 * </pre>
 * <p>
 * Individual schema names can be aliased. For example, to change the prefix of
 * the {@literal ville_Villeneuve-d'Ascq} schema to {@literal villeneuve}, use
 * the following config:
 * 
 * <pre>
 * <code> 
 * postgis:
 *   schemas:
 *     include:
 *     - schema: "*"
 *       prefix-tables: true
 *     - schema: "ville_Villeneuve-d'Ascq"
 *       alias: "villeneuve:
 * </code>
 * </pre>
 * 
 * This will result in a table {@literal locations} in that schema to be
 * published as {@literal villeneuve:locations} instead of
 * {@literal ville_Villeneuve-d'Ascq:locations}
 * <p>
 * A single schema can be configured to be un-prefixed, meaning its tables will
 * be published as collections without a schema prefix. For example, to remove
 * the {@literal public:} prefix for the {@literal public} schema, use:
 * 
 * <pre>
 * <code> 
 * postgis:
 *   schemas:
 *     include:
 *     - schema: "public"
 *       prefix-tables: false
 * </code>
 * </pre>
 * 
 * If the {@code *} schema wildcard, or more than one schema is configured with
 * {@code prefix-tables: false}, the application will fail to start.
 */
@Data
@EqualsAndHashCode(exclude = "includesBySchema")
@Validated
@ConfigurationProperties(prefix = "postgis.schemas")
public class PostgisSchemasConfiguration implements InitializingBean {

    public static final String EMPTY_PREFIX = "";

    private static final String ALL_SCHEMAS_WILDCARD = "*";

    private static final Set<String> HARD_SCHEMA_EXCLUDES = Set.of("pg_toast", "pg_catalog", "information_schema",
            "topology", "tiger");

    @NotNull
    private Refresh refresh = new Refresh();

    @NotEmpty
    private String delimiter = ":";

    /**
     * Map of schema names to include and configuration on whether to prefix table
     * names with the schema name, and in that case, an optional schema name alias.
     */
    @NotNull
    private List<SchemaConfiguration> include = List.of(SchemaConfiguration.all());

    /**
     * List of PostgreSQL schema names to exclude from automatic FeatureType
     * discovery. The exclude list takes precedence over includes.
     */
    @NotNull
    private List<String> exclude = List.of();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<String, SchemaConfiguration> includesBySchema;

    private Map<String, SchemaConfiguration> includesBySchema() {
        if (null == includesBySchema) {
            includesBySchema = include.stream()
                    .collect(Collectors.toMap(SchemaConfiguration::getSchema, Function.identity()));
        }
        return includesBySchema;
    }

    @VisibleForTesting
    public void clearCache() {
        this.includesBySchema = null;
    }

    @Data
    public static class Refresh {
        private boolean enabled = true;
        private Duration interval = Duration.ofSeconds(5);
    }

    @Data
    public static class SchemaConfiguration {
        @NotEmpty
        private String schema;
        private boolean prefixTables = true;
        private String alias;

        public SchemaConfiguration withSchema(@NonNull String schema) {
            SchemaConfiguration config = new SchemaConfiguration();
            config.setSchema(schema);
            config.setPrefixTables(prefixTables);
            config.setAlias(alias);
            return config;
        }

        public static SchemaConfiguration all() {
            SchemaConfiguration all = new SchemaConfiguration();
            all.setSchema(ALL_SCHEMAS_WILDCARD);
            all.setPrefixTables(true);
            all.setAlias(null);
            return all;
        }
    }

    public boolean includeSchema(String schema) {
        Map<String, SchemaConfiguration> bySchema = includesBySchema();
        return !excludeSchema(schema) && (bySchema.containsKey(ALL_SCHEMAS_WILDCARD) || bySchema.containsKey(schema));
    }

    private boolean excludeSchema(String schema) {
        return HARD_SCHEMA_EXCLUDES.contains(schema) || exclude.contains(ALL_SCHEMAS_WILDCARD)
                || exclude.contains(schema);
    }

    public String unalias(@NonNull String alias) {
        String schema;
        if (!StringUtils.hasText(alias)) {
            // there can be only a single config without prefixing
            Optional<SchemaConfiguration> unprefixed = include.stream().filter(c -> !c.isPrefixTables()).findFirst();
            if (unprefixed.isPresent()) {
                return unprefixed.orElseThrow().getSchema();
            }
            throw new IllegalStateException(
                    "Asking to unalias an empty prefix but there's no schema config with prefixing disabled");
        }
        schema = configForalias(alias).orElseGet(() -> configForSchema(alias)).getSchema();
        return schema;
    }

    private Optional<SchemaConfiguration> configForalias(@NonNull String alias) {
        return include.stream().filter(c -> alias.equals(c.getAlias())).findFirst();
    }

    public Optional<String> alias(String schema) {
        String alias = null;
        SchemaConfiguration schemaConfig = configForSchema(schema);
        if (schemaConfig.isPrefixTables()) {
            alias = schemaConfig.getAlias();
        }
        return Optional.ofNullable(alias);
    }

    private boolean shouldPrefix(String schema) {
        return configForSchema(schema).isPrefixTables();
    }

    @NonNull
    private SchemaConfiguration configForSchema(String schema) {
        SchemaConfiguration schemaConfig = includesBySchema().get(schema);
        if (null == schemaConfig) {
            schemaConfig = includesBySchema().get(ALL_SCHEMAS_WILDCARD);
            if (null == schemaConfig) {
                schemaConfig = new SchemaConfiguration();
                schemaConfig.setSchema(schema);
            } else {
                schemaConfig = schemaConfig.withSchema(schema);
            }
        }
        return schemaConfig;
    }

    public String prefix(String postgresSchema) {
        String prefix = alias(postgresSchema).orElseGet(() -> shouldPrefix(postgresSchema) ? postgresSchema : null);
        return Optional.ofNullable(prefix).map(pre -> pre + delimiter).orElse(EMPTY_PREFIX);
    }

    @NonNull
    public String extractPrefix(String schemaPrefixedTypeName) {
        int index = schemaPrefixedTypeName.indexOf(delimiter);
        if (index > 0) {
            return schemaPrefixedTypeName.substring(0, index);
        }
        return EMPTY_PREFIX;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        validate();
    }

    private void validate() {
        var bySchema = includesBySchema();
        SchemaConfiguration defaults = bySchema.get(ALL_SCHEMAS_WILDCARD);
        Assert.isTrue(null == defaults || defaults.isPrefixTables(),
                "The '*' schema wildcard can't have prefix-tables=false");

        List<String> unprefixing = bySchema.values().stream().filter(c -> !c.isPrefixTables())
                .map(SchemaConfiguration::getSchema).toList();

        Assert.isTrue(unprefixing.size() < 2, "Multiple schemas configured with prefix-tables=false: %s"
                .formatted(unprefixing.stream().sorted().collect(Collectors.joining(","))));
    }

    public static PostgisSchemasConfiguration defaultConfig() {
        return new PostgisSchemasConfiguration();
    }
}

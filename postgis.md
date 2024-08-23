## Configuration properties to define how to handle multiple PostgreSQL schemas to avoid Collection name clashes.

When running with the `postgis` Spring profile, the default configuration is to serve data
from all tables in all schemas, except for a few hard-coded schema names: `pg_toast`, `pg_catalog`, `information_schema`, `topology`, and `tiger`;
and to prefix all feature type names with the schema name and a `:` delimiter.

For example, a table `locations` in the `public` schema will be presented as `public:locations`.

Newly created database schemas, or removed schemas, will be noticed after a period defined by the `postgis.schemas.refresh.interval` config property.

If refresh is disabled (i.e. `postgis.schemas.refresh.enabled: false`), there will be no delay in noticing new or deleted PostgreSQL schemas,
but the performance will suffer, as the list of schemas will need to be queried upon each API request.

### Default configuration

The default configuration (i.e. if nothing is explicitly configured) is equivalent to the following:

```yaml
postgis:
  schemas:
    delimiter: ":"
    refresh:
      enabled: true
      interval: PT5S
    include:
    - schema: "*"
      prefix-tables: true
```

### Schema aliasing

Individual schema names can be aliased. For example, to change the prefix of the `ville_Villeneuve-d'Ascq` schema to `villeneuve`, use the following config:

```yaml
postgis:
  schemas:
    include:
    - schema: "*"
      prefix-tables: true
    - schema: "ville_Villeneuve-d'Ascq"
      alias: "villeneuve"
```

This will result in a table locations in that schema to be published as `villeneuve:locations` instead of `ville_Villeneuve-d'Ascq:locations`.

A single schema can be configured to be un-prefixed, meaning its tables will be published as Collections without a schema prefix.

For example, to remove the `public:` prefix for the `public` schema, use:

```yaml
postgis:
  schemas:
    include:
    - schema: "public"
      prefix-tables: false
```

If the `*` schema wildcard, or more than one schema is configured with `prefix-tables: false`, the application will fail to start.


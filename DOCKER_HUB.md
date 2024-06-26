# Quick reference

-    **Maintained by**:  
      [georchestra.org](https://www.georchestra.org/)

-    **Where to get help**:  
     the [geOrchestra Github repo](https://github.com/georchestra/georchestra), [IRC chat](https://matrix.to/#/#georchestra:osgeo.org), Stack Overflow

# Featured tags

- `latest`, `1.0.0`

# Quick reference

-	**Where to file issues**:  
     [https://github.com/georchestra/georchestra/issues](https://github.com/georchestra/georchestra/issues)

-	**Supported architectures**:   
     [`amd64`](https://hub.docker.com/r/amd64/docker/)

-	**Source of this description**:  
     [docs repo's `/` directory](https://github.com/georchestra/data-api/blob/main/DOCKER_HUB.md)

# What is `georchestra/data-api`

**Data-api** is an implementation of OGC API Features standard for geOrchestra. It allows:
- provide data in multiples formats like GeoJSON, CSV, Shapefile, etc.
- support filtering, sorting, paging and re-projection of data.


# How to use this image

Example of usage with docker-compose:

```yaml
  ogc-features:
    image: georchestra/data-api:latest
    depends_on:
      - postgis
    environment:
      SPRING_PROFILES_ACTIVE: postgis
      POSTGRES_HOST: postgis
      POSTGRES_PORT: 5432
      POSTGRES_DB: postgis
      POSTGRES_SCHEMA: opendataindex
      POSTGRES_USER: postgis
      POSTGRES_PASSWORD: postgis
      POSTGRES_POOL_MAXSIZE: 20
      POSTGRES_POOL_MINSIZE: 0
    ports:
      - 8080:8080
```

## Where is it built

This image is built using maven : `../mvnw package -Pdocker` in `data-api` repo.

# License

View [license information](https://www.georchestra.org/software.html) for the software contained in this image.

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

[//]: # (Some additional license information which was able to be auto-detected might be found in [the `repo-info` repository's georchestra/ directory]&#40;&#41;.)

As for any docker image, it is the user's responsibility to ensure that usages of this image comply with any relevant licenses for all software contained within.

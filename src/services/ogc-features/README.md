# OGC Features API for indexed data

Application service implementing [OGC API - FEATURES](https://ogcapi.ogc.org/features/) to serve both geospatial non geospatial indexed data.

##Build

See [top level](../../../README.md) README for build instructions.

## Run

### Development

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=sample-data
```

```bash
java -Dspring.profiles.active=sample-data -jar target/opendata-ogc-features-1.0-SNAPSHOT-bin.jar
```

### Docker

```bash
docker run -d -it --rm --name ogc-features -p 8080:8080 --env SPRING_PROFILES_ACTIVE=sample-data camptocamp/opendata-ogc-features:latest
docker logs ogc-features
```

## Features

## Data download

The "fetch features" endpoint (`/ogcapi/collections/{collectionId}/items`) allows to download the indexed datasets in several formats.

By spec, a `limit` query parameter must be included in the request to perform paging, and defaults to `10`.

#### Full dataset downloads

In order to perform a full data download, we allow a negative value for the `limit` query parameter to signify no limit in the number of records to include in the response.

##### Examples:

```bash
curl 'http://localhost:8080/ogcapi/collections/locations/items?limit=-1' -H 'accept: application/geo+json'

curl 'http://localhost:8080/ogcapi/collections/base-sirene-v3/items?limit=-1' \
  -H 'accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' \
  --output base-sirene-v3.xlsx
```


#### Output Formats

The following output formats are supported (using the standard HTTP `Accept` request header):

##### GeoJSON
Content-Type: `application/geo+json`

Ref: [https://geojson.org/](https://geojson.org/)

Streaming: **true**

This is the default output format.

##### CSV
Content-Type: `text/csv`

Ref: [https://www.ietf.org/rfc/rfc4180.txt](https://www.ietf.org/rfc/rfc4180.txt)

Streaming: **true**

Comma separated value, `RFC 4180` compliant, **

##### Shapefile
Content-Type: `application/x-shapefile`

Streaming: **false**

##### Excel 2007/OOXML
Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

Streaming: **true**

Formally, [Office Open XML](https://es.wikipedia.org/wiki/Office_Open_XML) format.

Contrary to all available encoders, the server-side generation of the OOXML document is **streaming**. This means the document starts downloading as soon as the data starts being processed.

We've developed a custom generator to do that, in order to avoid holding the whole "Workbook" representation in memory and/or using temporary files to write the document to while the data stream is encoded.

## Usage

Browse to [http://localhost:8080](http://localhost:8080), will redirect to [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html), where you'll find the Swagger UI for the API.





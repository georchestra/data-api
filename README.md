# Opendata services

## Build & run

### Requirements

Those components are created with the following requirements:
* Java 21 JDK
* Maven 3.8+
* Docker


### Building

Compile, test, install:

```shell script
./mvnw clean install
```

Build docker image:

```shell script
./mvnw clean package -Pdocker
```

### Running

```
docker compose up -d
```

Will launch the PostGIS container and the `ogc-features` application using PostGIS as its data source.

In order to run the `ogc-features` service against PostGIS, it must be run with the `postgis` Spring profile,
as well as the following environment variables with the appropriate values for the target environment:

```
SPRING_PROFILES_ACTIVE: postgis
POSTGRES_HOST: postgis
POSTGRES_PORT: 5432
POSTGRES_DB: postgis
POSTGRES_SCHEMA: opendataindex
POSTGRES_USER: postgis
POSTGRES_PASSWORD: postgis
POSTGRES_POOL_MAXSIZE: 20
POSTGRES_POOL_MINSIZE: 0
```

### Development

See the [OGC Features API](src/services/ogc-features/README.md) README for more information.


## Usage

Browse to [http://localhost:8080](http://localhost:8080), will redirect to [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html), where you'll find the Swagger UI for the API.

### Sample data

The same sample data used for development can be imported to PostGIS with the following command:

```
docker compose exec postgis /import-sample-data.sh
```

## Features

## Data download

The "fetch features" endpoint (`/ogcapi/collections/{collectionId}/items`) allows to download the indexed datasets in several formats.

By spec, a `limit` query parameter must be included in the request to perform paging, and defaults to `10`.

### Full dataset downloads

In order to perform a full data download, we allow a negative value for the `limit` query parameter to signify no limit in the number of records to include in the response.

For example:

```bash
curl 'http://localhost:8080/ogcapi/collections/locations/items?limit=-1' -H 'accept: application/geo+json'

curl 'http://localhost:8080/ogcapi/collections/base-sirene-v3/items?limit=-1' \
  -H 'accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' \
  --output base-sirene-v3.xlsx
```

### Full dataset downloads links

In order to support simple HTTP links to download full datasets, a query parameter `f` can be used with a short-name value for the desired output format, which can be one of:

* **geojson**, for GeoJSON output
* **shapefile**, for a ZIP file containing the Esri Shapefile export of the dataset
* **csv**, for a CSV file containing the Esri Shapefile export of the dataset
* **ooxml**, for an Excel 2007 (formally OOXML) export of the dataset

Examples:

[http:/localhost:8080/ogcapi/collections/locations/items?f=shapefile&limit=-1](http:/localhost:8080/ogcapi/collections/locations/items?f=shapefile&limit=-1)

[http:/localhost:8080/ogcapi/collections/locations/items?f=ooxml&limit=-1](http:/localhost:8080/ogcapi/collections/locations/items?f=ooxml&limit=-1)

These links are advertised by the collections request, for example:

[http://localhost:8080/ogcapi/collections](http://localhost:8080/ogcapi/collections)

```json
{
  "collections": [
    {
      "id": "locations",
      "title": "locations",
      "links": [
        {
          "href": "http:/localhost:8080/ogcapi/collections/locations/items?f=geojson",
          "rel": "items",
          "type": "application/geo+json",
          "title": "locations"
        },
        {
          "href": "http:/localhost:8080/ogcapi/collections/locations/items?f=geojson&limit=-1",
          "rel": "enclosure",
          "type": "application/geo+json",
          "title": "Bulk download (GeoJSON)"
        },
        {
          "href": "http:/localhost:8080/ogcapi/collections/locations/items?f=shapefile&limit=-1",
          "rel": "enclosure",
          "type": "application/x-shapefile",
          "title": "Bulk download (Esri Shapefile)"
        },
        {
          "href": "http:/localhost:8080/ogcapi/collections/locations/items?f=csv&limit=-1",
          "rel": "enclosure",
          "type": "text/csv;charset=UTF-8",
          "title": "Bulk download (Comma Separated Values)"
        },
        {
          "href": "http:/localhost:8080/ogcapi/collections/locations/items?f=ooxml&limit=-1",
          "rel": "enclosure",
          "type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "title": "Bulk download (Excel 2007 / OOXML)"
        }
      ],
      "itemType": "feature",
      "crs": [
        "http://www.opengis.net/def/crs/OGC/1.3/CRS84"
      ]
    }
  ]
}
```
#### Output Formats

The following output formats are supported (using the standard HTTP `Accept` request header):

---
##### GeoJSON
Content-Type: `application/geo+json`

Ref: [https://geojson.org/](https://geojson.org/)

Streaming: **true**

This is the default output format.

---
##### CSV
Content-Type: `text/csv`

Ref: [https://www.ietf.org/rfc/rfc4180.txt](https://www.ietf.org/rfc/rfc4180.txt)

Streaming: **true**

Comma separated value, `RFC 4180` compliant, **

---
##### Shapefile
Content-Type: `application/x-shapefile`

Streaming: **false**

---
##### Excel 2007/OOXML
Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

Streaming: **true**

Formally, [Office Open XML](https://es.wikipedia.org/wiki/Office_Open_XML) format.

Contrary to all available encoders, the server-side generation of the OOXML document is **streaming**. This means the document starts downloading as soon as the data starts being processed.

We've developed a custom generator to do that, in order to avoid holding the whole "Workbook" representation in memory and/or using temporary files to write the document to while the data stream is encoded.

## Bugs

TBD

## Roadmap

TBD

## Contributing

To set license header use:

```shell script
./mvnw license:format
```

package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;
import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;

@SpringBootTest(classes = OgcFeaturesApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("sample-data")
class CollectionsApiIT {

    private TestRestTemplate restTemplate;

    private @LocalServerPort int port;

    private String itemsUrlTemplate;

    @BeforeEach
    void setup() {
        restTemplate = new TestRestTemplate();
        itemsUrlTemplate = "http://localhost:%d/ogcapi/collections/{collection}/items?f={f}".formatted(port);
    }

    @Test
    void testGetItems_geojson() throws JSONException {
        final String url = itemsUrlTemplate + "&offset=0&limit=2";
        Map<String, String> urlVariables = Map.of("collection", "locations", "f", "geojson");
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, urlVariables);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/geo+json"));
        String expected = """
                {
                    "type":"FeatureCollection",
                    "numberMatched":16,
                    "numberReturned":2,
                    "features":[
                        {
                            "type":"Feature",
                            "@typeName":"locations",
                            "@id":"1",
                            "geometry":{"type":"Point","@name":"geom","@srs":"EPSG:4326","coordinates":[11.116667,46.066667]},
                            "properties":{"city":" Trento","number":140,"year":2002}
                        },
                        {
                            "type":"Feature",
                            "@typeName":"locations",
                            "@id":"10",
                            "geometry":{"type":"Point","@name":"geom","@srs":"EPSG:4326","coordinates":[2.183333,41.383333]},
                            "properties":{"city":" Barcelona","number":914,"year":2010}
                        }
                     ],
                     "links":[
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?f=geojson&offset=0&limit=2",
                             "rel":"self",
                             "type":"application/geo+json",
                             "title":"This document"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?f=geojson&offset=2&limit=2",
                             "rel":"next",
                             "type":"application/geo+json",
                             "title":"Next page"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=json",
                             "rel":"alternate",
                             "type":"application/json",
                             "title":"This document as JSON"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=shapefile",
                             "rel":"alternate",
                             "type":"application/x-shapefile",
                             "title":"This document as Esri Shapefile"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=csv",
                             "rel":"alternate",
                             "type":"text/csv;charset=UTF-8",
                             "title":"This document as Comma Separated Values"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=ooxml",
                             "rel":"alternate",
                             "type":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                             "title":"This document as Excel 2007 / OOXML"
                         }
                     ]
                }
                """
                .replaceAll("<port>", String.valueOf(port));

        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    void testGetItems_simplejson() throws JSONException {
        final String url = itemsUrlTemplate + "&offset=0&limit=2";
        Map<String, String> urlVariables = Map.of("collection", "locations", "f", "json");
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, urlVariables);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/json"));
        String expected = """
                {
                    "numberMatched":16,
                    "numberReturned":2,
                    "records":[
                        {
                            "@id":"1",
                            "city":" Trento",
                            "number":140,
                            "year":2002
                        },
                        {
                            "@id":"10",
                            "city":" Barcelona",
                            "number":914,
                            "year":2010
                        }
                     ],
                     "links":[
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?f=json&offset=0&limit=2",
                             "rel":"self",
                             "type":"application/json",
                             "title":"This document"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?f=json&offset=2&limit=2",
                             "rel":"next",
                             "type":"application/json",
                             "title":"Next page"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=geojson",
                             "rel":"alternate",
                             "type":"application/geo+json",
                             "title":"This document as GeoJSON"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=shapefile",
                             "rel":"alternate",
                             "type":"application/x-shapefile",
                             "title":"This document as Esri Shapefile"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=csv",
                             "rel":"alternate",
                             "type":"text/csv;charset=UTF-8",
                             "title":"This document as Comma Separated Values"
                         },
                         {
                             "href":"http://localhost:<port>/ogcapi/collections/locations/items?offset=0&limit=2&f=ooxml",
                             "rel":"alternate",
                             "type":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                             "title":"This document as Excel 2007 / OOXML"
                         }
                     ]
                }
                """
                .replaceAll("<port>", String.valueOf(port));

        JSONAssert.assertEquals(expected, response.getBody(), false);
    }
}

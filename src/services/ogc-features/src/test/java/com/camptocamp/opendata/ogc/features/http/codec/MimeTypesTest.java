package com.camptocamp.opendata.ogc.features.http.codec;

import static com.camptocamp.opendata.ogc.features.http.codec.MimeTypes.CSV;
import static com.camptocamp.opendata.ogc.features.http.codec.MimeTypes.GeoJSON;
import static com.camptocamp.opendata.ogc.features.http.codec.MimeTypes.JSON;
import static com.camptocamp.opendata.ogc.features.http.codec.MimeTypes.OOXML;
import static com.camptocamp.opendata.ogc.features.http.codec.MimeTypes.SHAPEFILE;
import static com.camptocamp.opendata.ogc.features.http.codec.MimeTypes.find;
import static com.camptocamp.opendata.ogc.features.http.codec.MimeTypes.findByShortName;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MimeTypesTest {

    @Test
    void testFindByShortName() {
        assertThat(findByShortName("json")).isPresent().get().isEqualTo(JSON);
        assertThat(findByShortName("geojson")).isPresent().get().isEqualTo(GeoJSON);
        assertThat(findByShortName("csv")).isPresent().get().isEqualTo(CSV);
        assertThat(findByShortName("ooxml")).isPresent().get().isEqualTo(OOXML);
        assertThat(findByShortName("shapefile")).isPresent().get().isEqualTo(SHAPEFILE);
    }

    @Test
    void testFindByMimeType() {

        assertThat(find("application/json")).isPresent().get().isEqualTo(JSON);
        assertThat(find("application/geo+json")).isPresent().get().isEqualTo(GeoJSON);
        assertThat(find("text/csv")).isPresent().get().isEqualTo(CSV);
        assertThat(find("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).isPresent().get()
                .isEqualTo(OOXML);
        assertThat(find("application/x-shapefile")).isPresent().get().isEqualTo(SHAPEFILE);
    }

    /**
     * Tests lookup of multivalued mimetypes.
     *
     * Some implementations can send an `Accept` http header as multivalued
     * comma-separated string.
     *
     * In this case, the code will take the first parsed value.
     */
    @Test
    void testFindMultivaluedMimeType() {
        assertThat(find("application/json, application.geo+json")).isPresent().get().isEqualTo(JSON);
    }

}

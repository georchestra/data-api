package com.camptocamp.opendata.ogc.features.http.codec;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MimeTypesTest {

    public @Test void testFindByShortName() {
        Optional<MimeTypes> ret = MimeTypes.findByShortName("geojson");

        assertTrue(ret.isPresent() && ret.get().equals(MimeTypes.GeoJSON));
    }

    public @Test void testFindByHeader() {
        Optional<MimeTypes> ret = MimeTypes.find("application/json");

        assertTrue(ret.isPresent() && ret.get().equals(MimeTypes.JSON));
    }

}

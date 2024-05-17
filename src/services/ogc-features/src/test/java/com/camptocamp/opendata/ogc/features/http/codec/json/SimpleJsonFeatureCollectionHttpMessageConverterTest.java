package com.camptocamp.opendata.ogc.features.http.codec.json;

import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.GeoToolsFeatureCollection;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.awt.*;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class SimpleJsonFeatureCollectionHttpMessageConverterTest {

    private SimpleFeatureCollection features() {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType());
        DefaultFeatureCollection col = new DefaultFeatureCollection();
        col.add(fb.buildFeature("1234", new java.sql.Timestamp(1714646315000L))); // ~ 2024-05-02 GMT+02

        return col;
    }

    private SimpleFeatureType featureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("test");
        builder.setNamespaceURI("http://test");
        builder.setSRS("EPSG:4326");
        builder.add("date", java.sql.Timestamp.class);
        builder.add("pointProperty", Point.class);

        return builder.buildFeatureType();
    }

    private FeatureCollection featureCollection() {
        Collection col = new Collection("1234", List.of());
        SimpleFeatureCollection features = features();
        GeoToolsFeatureCollection collection = new GeoToolsFeatureCollection(col, features);
        collection.setNumberMatched(1L);
        collection.setNumberReturned(1L);

        return collection;
    }

    @Test
    public void testJavaSqlTimeStampsAsISO8601Dates() throws Exception {
        SimpleJsonFeatureCollectionHttpMessageConverter converter = new SimpleJsonFeatureCollectionHttpMessageConverter();
        MockHttpOutputMessage message = new MockHttpOutputMessage();

        converter.writeInternal(featureCollection(), null, message);

        assertThat(message.getBodyAsString(), containsString("2024-05-02T"));
    }
}
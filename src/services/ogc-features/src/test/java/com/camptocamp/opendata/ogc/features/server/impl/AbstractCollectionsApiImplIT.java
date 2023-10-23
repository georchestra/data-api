package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;

import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.Collections;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;

public class AbstractCollectionsApiImplIT {

    protected @Autowired CollectionsApiImpl collectionsApi;

    protected @Autowired NativeWebRequest req;

    @Test
    public void testGetCollections() {

        var expected = Set.of("base-sirene-v3", "comptages-velo", "locations", "ouvrages-acquis-par-les-mediatheques");

        ResponseEntity<Collections> response = collectionsApi.getCollections();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Collections body = response.getBody();
        assertThat(body.getCollections().size()).isEqualTo(expected.size());

        var actual = body.getCollections().stream().map(Collection::getTitle).collect(Collectors.toSet());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetItems() {
        MockHttpServletRequest actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", 10, null, null, null);

        assertThat(response.getBody().getFeatures().toList().size()).isEqualTo(10);
    }

    @Test
    public void testGetItemsWithFilter() {
        MockHttpServletRequest actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", -1, null, null,
                "number = 140");

        assertThat(response.getBody().getFeatures().toList().size()).isEqualTo(1);
    }
}

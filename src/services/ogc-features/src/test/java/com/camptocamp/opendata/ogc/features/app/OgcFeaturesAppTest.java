package com.camptocamp.opendata.ogc.features.app;

import com.camptocamp.opendata.ogc.features.model.Collections;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.server.impl.CollectionsApiImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.NativeWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@EnableAutoConfiguration
@ActiveProfiles("sample-data")
public class OgcFeaturesAppTest {

    private @Autowired CollectionsApiImpl collectionsApi;

    private @Autowired NativeWebRequest req;
    @Test
    public void testCollectionApiInstanciation() {
        assertNotNull(collectionsApi);
    }

    @Test
    public void testGetCollections() {
        ResponseEntity<Collections> response = collectionsApi.getCollections();

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        assertThat(response.getBody().getCollections().size())
                .isEqualTo(3);
    }

    @Test
    public void testGetItems() {
        MockHttpServletRequest actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", 10,
                null, null, null);

        assertThat(response.getBody().getFeatures().toList().size())
                .isEqualTo(10);
    }

    @Test
    public void testGetItemsWithFilter() {
        MockHttpServletRequest actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", -1,
                null, null, "number = 140");

        assertThat(response.getBody().getFeatures().toList().size())
                .isEqualTo(1);
    }
}

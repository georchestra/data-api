package com.camptocamp.opendata.ogc.features.server.impl;

import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.Link;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OgcFeaturesApp.class)
@ActiveProfiles("sample-data")
public class CollectionsApiImplTest extends AbstractCollectionsApiImplIT {

    @Test
    public void testGetItemsLinks() {
        MockHttpServletRequest mockedReq = (MockHttpServletRequest) req.getNativeRequest();
        mockedReq.addParameter("f", "geojson");
        mockedReq.addHeader("Accept", "application/geo+json");

        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", //
                10, //
                null, //
                null, //
                null);

        List<Link> links = response.getBody().getLinks();
        assertTrue(links.get(1).getHref().contains("f=geojson"));
    }

    public @Test void testGetItemsLinksNoFParam() {
        MockHttpServletRequest mockedReq = (MockHttpServletRequest) req.getNativeRequest();
        mockedReq.removeParameter("f");
        mockedReq.addHeader("Accept", "application/json");

        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", //
                10, //
                null, //
                null, //
                null);

        List<Link> links = response.getBody().getLinks();
        assertTrue(links.get(1).getHref().contains("f=json"));
    }

    public @Test void testGetItemsLinksFParamAndDifferentHeaderParams() {
        MockHttpServletRequest mockedReq = (MockHttpServletRequest) req.getNativeRequest();
        mockedReq.addParameter("f", "ooxml");
        mockedReq.addHeader("Accept", "application/json");

        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures("locations", //
                10, //
                null, //
                null, //
                null);

        List<Link> links = response.getBody().getLinks();
        assertTrue(links.get(1).getHref().contains("f=ooxml"));
    }

}

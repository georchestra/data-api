package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.Link;

@SpringBootTest(classes = OgcFeaturesApp.class)
@ActiveProfiles("sample-data")
public class CollectionsApiImplTest extends AbstractCollectionsApiImplIT {

    @Override
    protected Comparator<GeodataRecord> fidComparator() {
        Comparator<GeodataRecord> fidComparator = (r1, r2) -> r1.getId().compareTo(r2.getId());
        return fidComparator;
    }

    @Test
    public void testGetItemsLinks() {
        actualRequest.setParameter("f", "geojson");
        actualRequest.removeHeader("Accept");
        actualRequest.addHeader("Accept", "application/geo+json");

        FeaturesQuery query = FeaturesQuery.of("locations").withLimit(10);
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures(query);

        List<Link> links = response.getBody().getLinks();
        Link nextLink = links.stream().filter(l -> "next".equals(l.getRel())).findFirst().orElseThrow();
        assertThat(nextLink.getHref()).contains("f=geojson");
    }

    public @Test void testGetItemsLinksNoFParam() {
        actualRequest.removeParameter("f");
        actualRequest.removeHeader("Accept");
        actualRequest.addHeader("Accept", "application/json");

        FeaturesQuery query = FeaturesQuery.of("locations").withLimit(10);
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures(query);

        List<Link> links = response.getBody().getLinks();
        Link nextLink = links.stream().filter(l -> "next".equals(l.getRel())).findFirst().orElseThrow();
        assertThat(nextLink.getHref()).contains("f=json");
    }

    public @Test void testGetItemsLinksFParamAndDifferentHeaderParams() {
        actualRequest.addHeader("Accept", "application/json");
        actualRequest.setParameter("f", "ooxml");

        ResponseEntity<FeatureCollection> response = collectionsApi
                .getFeatures(FeaturesQuery.of("locations").withLimit(2));
        List<Link> links = response.getBody().getLinks();
        Link nextLink = links.stream().filter(l -> "next".equals(l.getRel())).findFirst().orElseThrow();
        assertThat(nextLink.getHref()).contains("f=ooxml");
    }

}

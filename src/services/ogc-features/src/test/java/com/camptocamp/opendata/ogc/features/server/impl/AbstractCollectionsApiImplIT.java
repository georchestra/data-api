package com.camptocamp.opendata.ogc.features.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;

import com.camptocamp.opendata.model.DataQuery.SortBy;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.Collections;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.google.common.base.Splitter;

import lombok.Cleanup;

public abstract class AbstractCollectionsApiImplIT {

    protected @Autowired CollectionsApiImpl collectionsApi;

    protected @Autowired NativeWebRequest req;

    protected MockHttpServletRequest actualRequest;

    @BeforeEach
    void prepRequest() {
        actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");
    }

    @Test
    protected void testGetCollections() {

        var expected = Set.of("base-sirene-v3", "comptages-velo", "locations", "ouvrages-acquis-par-les-mediatheques");

        ResponseEntity<Collections> response = collectionsApi.getCollections();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Collections body = response.getBody();
        assertThat(body.getCollections()).hasSameSizeAs(expected);

        var actual = body.getCollections().stream().map(Collection::getTitle).collect(Collectors.toSet());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    protected void testGetItems() {
        FeaturesQuery query = FeaturesQuery.of("locations").withLimit(10);
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures(query);

        @Cleanup
        Stream<GeodataRecord> features = response.getBody().getFeatures();
        assertThat(features).hasSize(10);
    }

    @Test
    protected void testGetItemsWithFilter() {
        FeaturesQuery query = FeaturesQuery.of("locations").withFilter("number = 140");
        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures(query);

        @Cleanup
        Stream<GeodataRecord> features = response.getBody().getFeatures();
        assertThat(features.count()).isEqualTo(1);

    }

    @ParameterizedTest
    @ValueSource(strings = { "base-sirene-v3", "comptages-velo", "locations", "ouvrages-acquis-par-les-mediatheques" })
    protected void testGetItems_paging_natural_order(String layerName) {
        FeaturesQuery query = FeaturesQuery.of(layerName);

        Comparator<GeodataRecord> fidComparator = fidComparator();
        testPagingConsistency(query, fidComparator);
    }

    protected abstract Comparator<GeodataRecord> fidComparator();

    @ParameterizedTest
    @ValueSource(strings = { //
            "base-sirene-v3:+nic,-siren", //
            "comptages-velo:commune,date et heure", //
            "locations:-city", //
            "ouvrages-acquis-par-les-mediatheques:-type de document,editeur" })
    protected void testGetItems_paging_mixed_order(String layerAndSortSpec) {
        String layerName = layerAndSortSpec.substring(0, layerAndSortSpec.indexOf(':'));
        String sortSpec = layerAndSortSpec.substring(1 + layerAndSortSpec.indexOf(':'));

        List<String> sortby = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(sortSpec);

        FeaturesQuery query = FeaturesQuery.of(layerName).withSortby(sortby);

        Comparator<GeodataRecord> comparator = sortSpecComparator(query.sortBy());
        testPagingConsistency(query, comparator);
    }

    protected Comparator<GeodataRecord> sortSpecComparator(List<SortBy> sortBy) {
        Comparator<GeodataRecord> comparator = sortBy.get(0);
        for (int i = 1; i < sortBy.size(); i++) {
            comparator = comparator.thenComparing(sortBy.get(i));
        }
        return comparator.thenComparing(fidComparator());
    }

    private void testPagingConsistency(FeaturesQuery query, Comparator<GeodataRecord> comparator) {

        final int total = collectionsApi.getFeatures(FeaturesQuery.of(query.getCollectionId())).getBody()
                .getNumberMatched().intValue();
        final int pageSize = total / 11;
        final int lastPageSize = Math.min(pageSize, total % pageSize);
        final int pages = total / pageSize + (lastPageSize > 0 ? 1 : 0);
        query = query.withLimit(pageSize);

        final Pageable last = Pageable.ofSize(lastPageSize == 0 ? pageSize : lastPageSize).withPage(pages - 1);

        Pageable page = Pageable.ofSize(pageSize);
        Set<String> idsReturned = new LinkedHashSet<>();
        List<GeodataRecord> records = new ArrayList<>();

        for (int p = 0; p < pages; p++, page = page.next()) {
            int offset = (int) page.getOffset();
            query = query.withOffset(offset);
            FeatureCollection collection = collectionsApi.getFeatures(query).getBody();
            assertThat(collection.getNumberMatched())
                    .as("numberMatched should be the number of features matching the query filter").isEqualTo(total);

            int pageNumber = page.getPageNumber();
            if (pageNumber == last.getPageNumber()) {
                assertThat(collection.getNumberReturned()).as("numberReturned should be equal to last page size")
                        .isEqualTo(last.getPageSize());
            } else {
                assertThat(collection.getNumberReturned()).as("numberReturned should be equal to page size")
                        .isEqualTo(page.getPageSize());
            }

            @Cleanup
            Stream<GeodataRecord> features = collection.getFeatures();
            features.forEach(rec -> {
                String id = rec.getId();
                assertThat(idsReturned).as("Duplicate feature returned on page " + pageNumber).doesNotContain(id);
                idsReturned.add(id);
                records.add(rec);
            });
        }
//		assertThat(records).isEqualTo(sorted);
    }

    @Test
    protected void testGetItemsWithFilter_property_name_with_spaces() throws Exception {
        final String collectionName = "base-sirene-v3";
        final String propetyName = "indice de répétition de l'établissement";

        MockHttpServletRequest actualRequest = (MockHttpServletRequest) req.getNativeRequest();
        actualRequest.addHeader("Accept", "application/json");

        ResponseEntity<FeatureCollection> response = collectionsApi.getFeatures(FeaturesQuery.of(collectionName));

        FeatureCollection body = response.getBody();

        @Cleanup
        Stream<GeodataRecord> features = body.getFeatures();
        int expected = (int) features.filter(c -> "B".equals(c.getProperty(propetyName).orElseThrow().getValue()))
                .count();
        assertThat(expected).isPositive();

        final String cqlFilter = "\"%s\" = 'B'".formatted(propetyName);

        assertECQL_FilterCanBeParsed(propetyName, cqlFilter);

        response = collectionsApi.getFeatures(FeaturesQuery.of(collectionName).withFilter(cqlFilter));
        body = response.getBody();
        @Cleanup
        Stream<GeodataRecord> features1 = body.getFeatures();
        assertThat(features1.count()).isEqualTo(expected);

        response = collectionsApi
                .getFeatures(FeaturesQuery.of(collectionName).withFilter(cqlFilter).withLimit(expected));

        body = response.getBody();

        @Cleanup
        Stream<GeodataRecord> features2 = body.getFeatures();
        assertThat(features2.count()).isEqualTo(expected);
    }

    private void assertECQL_FilterCanBeParsed(final String propetyName, final String filter) {
        Filter ff = assertDoesNotThrow(() -> ECQL.toFilter(filter));
        assertThat(ff).isInstanceOf(PropertyIsEqualTo.class);
        Expression expression1 = ((PropertyIsEqualTo) ff).getExpression1();
        assertThat(expression1).isInstanceOf(PropertyName.class);
        assertThat(((PropertyName) expression1).getPropertyName()).isEqualTo(propetyName);
    }
}

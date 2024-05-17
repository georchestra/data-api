package com.camptocamp.opendata.ogc.features.server.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.DataQuery.SortBy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

@Value
@With
@RequiredArgsConstructor
public class FeaturesQuery {

    private static final FilterFactory FILTER_FACTORY = CommonFactoryFinder.getFilterFactory();

    private @NonNull String collectionId;
    private Integer offset;
    private Integer limit;
    private List<BigDecimal> bbox;
    private String datetime;
    private String filter;
    private String filterLang;
    private String filterCrs;
    private List<String> sortby;
    private String crs;

    public static FeaturesQuery of(String collectionId) {
        return new FeaturesQuery(collectionId, null, null, null, null, null, null, null, null, null);
    }

    public FeaturesQuery withBbox(double minx, double miny, double maxx, double maxy) {
        return withBbox(List.of(BigDecimal.valueOf(minx), BigDecimal.valueOf(miny), BigDecimal.valueOf(maxx),
                BigDecimal.valueOf(maxy)));
    }

    public List<SortBy> sortBy() {
        return Optional.ofNullable(getSortby()).orElse(List.of()).stream().map(this::toSortBy).toList();
    }

    private DataQuery.SortBy toSortBy(String s) {
        String propertyName = s;
        boolean ascending = true;
        if (s.startsWith("+")) {
            propertyName = propertyName.substring(1);
        } else if (s.startsWith("-")) {
            ascending = false;
            propertyName = propertyName.substring(1);
        }
        return new DataQuery.SortBy(propertyName, ascending);
    }

}

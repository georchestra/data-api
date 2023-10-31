package com.camptocamp.opendata.ogc.features.server.impl;

import java.math.BigDecimal;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

@Value
@With
@RequiredArgsConstructor
public class FeaturesQuery {

    private @NonNull String collectionId;
    private Integer offset;
    private Integer limit;
    private List<BigDecimal> bbox;
    private String datetime;
    private String filter;
    private String filterLang;

    public static FeaturesQuery of(String collectionId) {
        return new FeaturesQuery(collectionId, null, null, null, null, null, null);
    }

    public FeaturesQuery withBbox(double minx, double miny, double maxx, double maxy) {
        return withBbox(List.of(BigDecimal.valueOf(minx), BigDecimal.valueOf(miny), BigDecimal.valueOf(maxx),
                BigDecimal.valueOf(maxy)));
    }
}

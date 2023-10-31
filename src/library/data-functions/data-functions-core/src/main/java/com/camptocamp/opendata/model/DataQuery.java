package com.camptocamp.opendata.model;

import java.net.URI;
import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;

@Value
@With
@Builder
@Accessors(chain = true)
public class DataQuery {

    private @NonNull DataSource source;
    private String layerName;
    private Integer offset;
    private Integer limit;
    private String filter;
    private @NonNull List<SortBy> sortBy;

    public static DataQuery fromUri(URI dataUri) {
        DataSource dataSource = DataSource.fromUri(dataUri);
        return DataQuery.builder().source(dataSource).sortBy(List.of()).build();
    }

    public static record SortBy(String propertyName, boolean ascending) {

    }
}

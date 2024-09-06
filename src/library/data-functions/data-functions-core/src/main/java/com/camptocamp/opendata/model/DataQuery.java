package com.camptocamp.opendata.model;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Comparator;
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

    @NonNull DataSource source;
    String layerName;
    Integer offset;
    Integer limit;
    String filter;
    @NonNull List<SortBy> sortBy;
    String targetCrs;
    List<BigDecimal> bbox;

    public static DataQuery fromUri(URI dataUri) {
        DataSource dataSource = DataSource.fromUri(dataUri);
        return DataQuery.builder().source(dataSource).sortBy(List.of()).build();
    }

    public record SortBy(String propertyName, boolean ascending) implements Comparator<GeodataRecord> {

    @SuppressWarnings("unchecked")
    @Override
    public int compare(GeodataRecord o1, GeodataRecord o2) {
        Comparable<Object> p1 = (Comparable<Object>) o1.getProperty(propertyName).map(SimpleProperty::getValue)
                .orElse(null);
        Comparable<Object> p2 = (Comparable<Object>) o2.getProperty(propertyName).map(SimpleProperty::getValue)
                .orElse(null);

        if (p1 == null && p2 == null)
            return 0;
        if (p1 == null)
            return ascending ? 1 : -1;
        if (p2 == null)
            return ascending ? -1 : 1;

        if (ascending)
            return p1.compareTo(p2);
        return p2.compareTo(p1);
    }

}}

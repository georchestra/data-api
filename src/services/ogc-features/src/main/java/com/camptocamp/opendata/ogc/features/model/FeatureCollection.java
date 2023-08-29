package com.camptocamp.opendata.ogc.features.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.format.annotation.DateTimeFormat;

import com.camptocamp.opendata.model.GeodataRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "type", "timeStamp", "numberMatched", "numberReturned", "features", "links" })
public interface FeatureCollection {

    @JsonIgnore
    default Optional<SimpleFeatureCollection> getOriginalContents() {
        return Optional.empty();
    }

    default String getType() {
        return "FeatureCollection";
    }

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    default OffsetDateTime getTimeStamp() {
        return OffsetDateTime.now();
    }

    @JsonIgnore
    Collection getCollection();

    Long getNumberMatched();

    Long getNumberReturned();

    Stream<GeodataRecord> getFeatures();

    List<Link> getLinks();
}

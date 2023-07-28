package com.camptocamp.opendata.ogc.features.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.format.annotation.DateTimeFormat;

import com.camptocamp.opendata.model.GeodataRecord;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;
import reactor.core.publisher.Flux;

@Data
@Accessors(chain = true)
public class FeatureCollection {

    private final String type = "FeatureCollection";

    private Supplier<Flux<GeodataRecord>> features = Flux::empty;

    private List<Link> links;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime timeStamp;

    private Integer numberMatched;

    private Integer numberReturned;

    @JsonProperty("features")
    public Flux<GeodataRecord> getFeatures() {
        return features.get();
    }

}

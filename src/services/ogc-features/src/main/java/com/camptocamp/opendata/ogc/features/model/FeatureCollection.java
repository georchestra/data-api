package com.camptocamp.opendata.ogc.features.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.format.annotation.DateTimeFormat;

import com.camptocamp.opendata.model.GeodataRecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeatureCollection {

    private final String type = "FeatureCollection";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime timeStamp;

    private Long numberMatched;
    private Long numberReturned;

    // private Supplier<Stream<? extends GeodataRecord>> features = Stream::empty;
    private List<? extends GeodataRecord> features = List.of();

    private List<Link> links;
}

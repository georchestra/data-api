package com.camptocamp.opendata.indexing.processor;

import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;

import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.processor.StandardProcessors;
import com.camptocamp.opendata.processor.geotools.GeoToolsProcessors;

@Service
public class Processors {

    private @Autowired StandardProcessors stdProcessors;
    private @Autowired GeoToolsProcessors gtProcessors;

    public Function<Stream<GeodataRecord>, Stream<GeodataRecord>> toWgs84() {
        return reproject("EPSG:4326");
    }

    public Function<Stream<GeodataRecord>, Stream<GeodataRecord>> reproject(//
            @NonNull String targetSrs) {

        return gtProcessors.reproject(targetSrs);
    }

    public Function<Stream<Geometry>, Stream<Geometry>> reproject(//
            @NonNull String sourceSrs, //
            @NonNull String targetSrs) {

        return gtProcessors.reproject(sourceSrs, targetSrs);
    }

    public Function<Stream<GeodataRecord>, Stream<GeodataRecord>> renameProperty(//
            @NonNull String propertyName, //
            @NonNull String newPropertyname) {

        return stdProcessors.renameProperty(propertyName, newPropertyname);
    }
}

package com.camptocamp.opendata.ogc.features.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.camptocamp.opendata.processor.geotools.GeoToolsProcessors;
import io.micrometer.common.util.StringUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.producer.geotools.FeatureToRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class GeoToolsFeatureCollection implements FeatureCollection {

    private final @NonNull Function<SimpleFeature, GeodataRecord> featureMapper = new FeatureToRecord();

    private final @NonNull @Getter Collection collection;
    private final @NonNull SimpleFeatureCollection features;
    private final @NonNull GeoToolsProcessors gtProcessors = new GeoToolsProcessors();

    private @Setter @Getter Long numberMatched;
    private @Setter @Getter Long numberReturned;
    private @JsonIgnore @Setter @Getter String targetCrs;
    private final @Getter List<Link> links = new ArrayList<>();

    @JsonIgnore
    public @Override Optional<SimpleFeatureCollection> getOriginalContents() {
        return Optional.of(features);
    }

    @Override
    public Stream<GeodataRecord> getFeatures() {
        SimpleFeatureIterator it = features.features();
        Iterator<SimpleFeature> iterator = DataUtilities.iterator(it);
        int characteristics = Spliterator.DISTINCT | Spliterator.NONNULL;
        Spliterator<SimpleFeature> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
        Stream<SimpleFeature> stream = StreamSupport.stream(spliterator, false);
        stream = stream.onClose(it::close);
        if (!StringUtils.isEmpty(targetCrs)){
            return gtProcessors.reproject(String.format("EPSG:%s", targetCrs)).apply(stream.map(featureMapper));
        }
        return stream.map(featureMapper);
    }
}

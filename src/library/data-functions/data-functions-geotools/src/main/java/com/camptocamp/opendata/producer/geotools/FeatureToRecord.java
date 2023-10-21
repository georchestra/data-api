package com.camptocamp.opendata.producer.geotools;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.model.GeometryProperty;
import com.camptocamp.opendata.model.SimpleProperty;
import com.google.common.base.Predicates;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeatureToRecord implements Function<SimpleFeature, GeodataRecord> {

    private Map<CoordinateReferenceSystem, String> crsToSrs = new IdentityHashMap<>();

    @Override
    public GeodataRecord apply(SimpleFeature f) {
        log.trace("Mapping feature {} to GeodataRecord", f.getID());
        GeodataRecord.Builder builder = GeodataRecord.builder();
        String typeName = f.getFeatureType().getTypeName();
        builder.typeName(typeName);
        builder.id(stripTypeNameFromFeatureId(typeName, f.getID()));
        builder.geometry(defaultGeometry(f));
        builder.properties(simpleProperties(f));
        return builder.build();
    }

    /**
     * Removes the feature type name from the feature id and returns it.
     * <p>
     * GeoTools has an abstraction leak on its data access API by which it prepends
     * the FeatureType name to the Feature ids. We don't need that.
     */
    private String stripTypeNameFromFeatureId(String typeName, String fid) {
        if (fid.startsWith(typeName) && fid.length() > typeName.length() + 1 && fid.charAt(typeName.length()) == '.') {
            fid = fid.substring(typeName.length() + 1);
        }
        return fid;
    }

    private List<? extends SimpleProperty<?>> simpleProperties(SimpleFeature f) {

        Predicate<? super Property> notDefaultGeom = Predicates.alwaysTrue();
        if (null != f.getFeatureType().getGeometryDescriptor()) {
            GeometryDescriptor d = f.getFeatureType().getGeometryDescriptor();
            notDefaultGeom = p -> !p.getName().equals(d.getName());
        }

        return f.getProperties().stream().filter(notDefaultGeom).map(this::toProperty).toList();
    }

    private SimpleProperty<? extends Object> toProperty(Property property) {
        return property instanceof GeometryAttribute ? geometryProperty((GeometryAttribute) property)
                : simpleProperty(property);
    }

    private SimpleProperty<?> simpleProperty(Property att) {
        SimpleProperty<?> p = SimpleProperty.builder()//
                .name(att.getName().getLocalPart())//
                .value(att.getValue())//
                .build();
        return p;
    }

    private GeometryProperty defaultGeometry(SimpleFeature f) {
        GeometryAttribute defaultGeometry = f.getDefaultGeometryProperty();
        return geometryProperty(defaultGeometry);
    }

    private GeometryProperty geometryProperty(GeometryAttribute geometry) {
        if (null == geometry) {
            return null;
        }
        String name = geometry.getName().getLocalPart();
        Geometry value = (Geometry) geometry.getValue();
        CoordinateReferenceSystem crs = geometry.getType().getCoordinateReferenceSystem();
        String srs = crsToSrs.computeIfAbsent(crs, this::toSrs);
        return GeometryProperty.builder().name(name).value(value).srs(srs).build();
    }

    private String toSrs(CoordinateReferenceSystem crs) {
        if (null == crs)
            return "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        boolean fullScan = true;
        try {
            return CRS.lookupIdentifier(crs, fullScan);
        } catch (FactoryException e) {
            log.warn("Unable to determine SRS identifer, will use null for {}", crs);
            return null;
        }
    }
}

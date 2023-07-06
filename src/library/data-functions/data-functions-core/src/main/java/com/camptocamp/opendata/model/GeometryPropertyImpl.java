package com.camptocamp.opendata.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import org.locationtech.jts.geom.Geometry;

@Value
@With
@Builder
public class GeometryPropertyImpl implements GeometryProperty {

    private @NonNull String name;
    private Geometry value;
    private String srs;
}

package com.camptocamp.opendata.model;

import org.locationtech.jts.geom.Geometry;

import com.camptocamp.opendata.model.GeometryPropertyImpl.GeometryPropertyImplBuilder;

public interface GeometryProperty extends SimpleProperty<Geometry> {

    String getSrs();

    @Override
    GeometryProperty withValue(Geometry value);

    GeometryProperty withSrs(String srs);

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private final GeometryPropertyImplBuilder impl = GeometryPropertyImpl.builder();

        public Builder name(String name) {
            impl.name(name);
            return this;
        }

        public Builder srs(String srs) {
            impl.srs(srs);
            return this;
        }

        public Builder value(Geometry value) {
            impl.value(value);
            return this;
        }

        public GeometryProperty build() {
            return impl.build();
        }
    }
}

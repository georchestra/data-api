package com.camptocamp.geotools.data.decorate;

import org.geotools.api.data.SimpleFeatureSource;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class DecoratingFeatureSource implements SimpleFeatureSource {

    @Delegate
    protected final @NonNull SimpleFeatureSource delegate;
}

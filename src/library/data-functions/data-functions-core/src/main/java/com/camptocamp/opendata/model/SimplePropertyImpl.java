package com.camptocamp.opendata.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
class SimplePropertyImpl<T> implements SimpleProperty<T> {
    private @NonNull String name;
    private T value;
}

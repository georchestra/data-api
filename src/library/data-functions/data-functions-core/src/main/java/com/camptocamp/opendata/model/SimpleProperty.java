package com.camptocamp.opendata.model;

import com.camptocamp.opendata.model.SimplePropertyImpl.SimplePropertyImplBuilder;

import lombok.NonNull;

public interface SimpleProperty<T> {

    @NonNull
    String getName();

    T getValue();

    SimpleProperty<T> withName(String name);

    SimpleProperty<T> withValue(T value);

    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    static class Builder<T> {
        private SimplePropertyImplBuilder<T> impl = SimplePropertyImpl.builder();

        public Builder<T> name(String name) {
            impl.name(name);
            return this;
        }

        public Builder<T> value(T value) {
            impl.value(value);
            return this;
        }

        public SimpleProperty<T> build() {
            return impl.build();
        }
    }
}

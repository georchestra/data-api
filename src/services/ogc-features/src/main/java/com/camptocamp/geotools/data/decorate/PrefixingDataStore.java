package com.camptocamp.geotools.data.decorate;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.geotools.api.data.DataStore;

import lombok.NonNull;

/**
 * {@link RenamingDataStore} whose rename/undo functions apply or remove a given
 * prefix to all FeatureType names
 */
public class PrefixingDataStore extends RenamingDataStore {

    public PrefixingDataStore(@NonNull DataStore delegate, @NonNull Supplier</* Nullable */String> typeNamePrefix) {
        super(delegate, rename(typeNamePrefix), undo(typeNamePrefix));
    }

    /**
     * Function to apply the FeatureType name prefix as the renaming function of
     * RenamingDataStore
     */
    private static @NonNull UnaryOperator<String> rename(Supplier<String> typeNamePrefix) {
        return name -> isEmpty(typeNamePrefix.get()) ? name : "%s%s".formatted(typeNamePrefix.get(), name);
    }

    /**
     * Function remote the FeatureType name prefix as the undo renaming function of
     * RenamingDataStore
     */
    private static @NonNull UnaryOperator<String> undo(Supplier<String> typeNamePrefix) {
        return prefixedName -> isEmpty(typeNamePrefix.get()) ? prefixedName
                : prefixedName.substring(typeNamePrefix.get().length());
    }
}

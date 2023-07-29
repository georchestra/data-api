package com.camptocamp.opendata.producer.geotools;

import java.util.Arrays;
import java.util.List;
import lombok.Data;

/**
 * Global GeoPackage protocol preferences
 */
@Data
class GeoPackageFormatProperties {

    /**
     * File extensions (without dot, e.g. 'gpkg') used to assume the resource
     * pointed out by a dataset URI is a GeoPackage file.
     * 
     * Defaults to [gpkg, geopkg, geopackage]
     */
    List<String> fileExtensions = Arrays.asList("gpkg", "geopkg", "geopackage");
}

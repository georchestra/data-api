package com.camptocamp.opendata.ogc.features.autoconfigure.geotools;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class GeoToolsStaticContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.setProperty("org.geotools.referencing.forceXY", "true");
    }
}

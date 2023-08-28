package com.camptocamp.opendata.ogc.features.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.camptocamp.opendata.ogc.features.autoconfigure.GeoToolsStaticContextInitializer;

@SpringBootApplication
public class OgcFeaturesApp {

    public static void main(String... args) {
        try {
            SpringApplication app = new SpringApplication(OgcFeaturesApp.class);
            app.addInitializers(new GeoToolsStaticContextInitializer());
            app.run(args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

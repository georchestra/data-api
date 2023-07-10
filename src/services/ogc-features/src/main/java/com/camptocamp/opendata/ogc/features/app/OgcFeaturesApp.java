package com.camptocamp.opendata.ogc.features.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OgcFeaturesApp {

    public static void main(String... args) {
        try {
            SpringApplication.run(OgcFeaturesApp.class, args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

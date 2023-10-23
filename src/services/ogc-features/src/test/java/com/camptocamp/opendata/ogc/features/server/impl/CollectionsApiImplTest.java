package com.camptocamp.opendata.ogc.features.server.impl;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.camptocamp.opendata.ogc.features.app.OgcFeaturesApp;

@SpringBootTest(classes = OgcFeaturesApp.class)
@ActiveProfiles("sample-data")
public class CollectionsApiImplTest extends AbstractCollectionsApiImplIT {
}

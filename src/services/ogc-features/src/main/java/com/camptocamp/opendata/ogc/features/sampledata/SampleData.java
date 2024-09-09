package com.camptocamp.opendata.ogc.features.sampledata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.data.DataStore;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

import com.camptocamp.opendata.ogc.features.repository.DataStoreProvider;
import com.google.common.io.ByteStreams;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts sample data to a temporary directory and deletes at context
 * shutdown, since the geotools CSV datastore does not support URL resources,
 * only Files
 */
@Slf4j(topic = "com.camptocamp.opendata.ogc.features.autoconfigure.geotools")
public class SampleData implements DataStoreProvider, DisposableBean {

    private Path tempDirectory;

    private DataStore dataStore;

    @Override
    public DataStore get() {
        if (null == dataStore) {
            synchronized (this) {
                if (null == dataStore) {
                    try {
                        create();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
        return dataStore;
    }

    @Override
    public void reInit() {
        try {
            dispose();
            create();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void create() throws IOException {
        tempDirectory = Files.createTempDirectory("ogc-features-sample-data");
        dataStore = new MemoryDataStore();
        log.info("Extracting sample data to {}", tempDirectory);

        File sd = copyToTempDir("sample-datasets.gpkg");

        final GeoPkgDataStoreFactory factory = new GeoPkgDataStoreFactory();
        Map<String, Object> params = Map.of(//
                GeoPkgDataStoreFactory.DBTYPE.key, "geopkg", //
                GeoPkgDataStoreFactory.DATABASE.key, sd, //
                // Whether to return only tables listed as features in gpkg_contents, or give
                // access to all other tables
                // (careful, enabling this and then writing might cause the GeoPackage not to
                // conform to spec any longer, use at your discretion)
                GeoPkgDataStoreFactory.CONTENTS_ONLY.key, false//
        );

        JDBCDataStore sdds = factory.createDataStore(params);
        var typeNames = Stream.of(sdds.getTypeNames()).collect(Collectors.toCollection(TreeSet::new));
        var expected = Set.of("base-sirene-v3", "comptages-velo", "locations", "ouvrages-acquis-par-les-mediatheques");
        try {
            for (String typeName : expected) {
                Assert.isTrue(typeNames.contains(typeName),
                        "Expected FeatureType %s not found in gpkg".formatted(typeName));
                ContentFeatureSource featureSource = sdds.getFeatureSource(typeName);
                ContentFeatureCollection features = featureSource.getFeatures();
                ((MemoryDataStore) dataStore).addFeatures(features);
            }
        } finally {
            sdds.dispose();
        }
    }

    @Override
    public void destroy() throws IOException {
        dispose();
    }

    private void dispose() throws IOException {
        if (null != dataStore) {
            dataStore.dispose();
            if (tempDirectory != null && Files.isDirectory(tempDirectory)) {
                log.info("Deleting sample data directory {}", tempDirectory);
                FileSystemUtils.deleteRecursively(tempDirectory);
            }
            dataStore = null;
            tempDirectory = null;
        }
    }

    private File copyToTempDir(String fileName) throws IOException {
        String uri = "/sample-data/" + fileName;

        Path target = tempDirectory.resolve(fileName);
        try (var from = getClass().getResourceAsStream(uri); var to = new FileOutputStream(target.toFile())) {

            Objects.requireNonNull(from, () -> "resource " + uri + " not found");
            ByteStreams.copy(from, to);
        }
        return target.toFile();
    }
}
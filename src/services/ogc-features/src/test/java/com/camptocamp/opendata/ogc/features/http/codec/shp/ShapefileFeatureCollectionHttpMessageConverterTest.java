package com.camptocamp.opendata.ogc.features.http.codec.shp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDirectoryFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.mock.http.MockHttpOutputMessage;

import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.GeoToolsFeatureCollection;

import lombok.SneakyThrows;

class ShapefileFeatureCollectionHttpMessageConverterTest {

    @TempDir
    Path unzipFolder;

    ShapefileFeatureCollectionHttpMessageConverter converter;
    MockHttpOutputMessage output;

    @BeforeEach
    void setup() {
        converter = new ShapefileFeatureCollectionHttpMessageConverter();
        output = new MockHttpOutputMessage();
    }

    @Test
    void testSimpleCollection() throws Exception {

        FeatureCollection collection = featureCollection();

        Type unused = null;
        converter.writeInternal(collection, unused, output);

        SimpleFeatureCollection expected = collection.getOriginalContents().orElseThrow();
        SimpleFeatureCollection actual = extractShapefile(expected.getSchema().getTypeName(), output.getBodyAsBytes());

        assertThat(actual.size()).isEqualTo(expected.size());
        assertFeatureCollectionContents(expected, actual, "date");
    }

    private void assertFeatureCollectionContents(SimpleFeatureCollection expected, SimpleFeatureCollection actual,
            String sortProperty) {
        List<SimpleFeature> fExpected = sortedContents(expected, sortProperty);
        List<SimpleFeature> fActual = sortedContents(actual, sortProperty);
        assertThat(fActual).hasSameSizeAs(fExpected);
        for (int i = 0; i < fExpected.size(); i++) {
            assertFeatureContents(fExpected.get(i), fActual.get(i));
        }
    }

    private void assertFeatureContents(SimpleFeature expected, SimpleFeature actual) {
        SimpleFeatureType schema = actual.getFeatureType();
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
        assertThat(actual.getDefaultGeometry()).isEqualTo(expected.getDefaultGeometry());
        for (AttributeDescriptor att : schema.getAttributeDescriptors()) {
            if (att == geometryDescriptor)
                continue;
            String attName = att.getLocalName();
            Object ev = expected.getAttribute(attName);
            Object av = actual.getAttribute(attName);
            assertThat(av).isEqualTo(ev);
        }

    }

    @SneakyThrows(IOException.class)
    private List<SimpleFeature> sortedContents(SimpleFeatureCollection collection, String sortProperty) {

        @SuppressWarnings("unchecked")
        Comparator<SimpleFeature> comparator = Comparator
                .comparing(f -> (Comparable<Object>) ((SimpleFeature) f).getAttribute(sortProperty));

        comparator = Comparator.nullsFirst(comparator);

        return new ListFeatureCollection(collection).stream().sorted(comparator).toList();
    }

    @SneakyThrows(IOException.class)
    private SimpleFeatureCollection extractShapefile(String typeName, byte[] shapeZip) {
        ShapefileDataStore result = unzipAndGetShapefile(typeName, shapeZip);
        try {
            ContentFeatureCollection actual = result.getFeatureSource().getFeatures();
            return new ListFeatureCollection(actual);
        } finally {
            result.dispose();
        }
    }

    @SneakyThrows(IOException.class)
    private ShapefileDataStore unzipAndGetShapefile(String typeName, byte[] shapeZip) {
        Set<Path> unzipped = unzip(shapeZip);

        Set<Path> expectedFiles = Stream.of("%s.shp", "%s.dbf", "%s.prj", "%s.shx").map(s -> s.formatted(typeName))
                .map(unzipFolder::resolve).collect(Collectors.toSet());

        assertThat(unzipped).isEqualTo(expectedFiles);

        Path shp = unzipFolder.resolve("%s.shp".formatted(typeName));
        return ShapefileFeatureCollectionHttpMessageConverter.createDataStore(shp);
    }

    private Set<Path> unzip(byte[] shapeZip) throws IOException {
        ZipInputStream zipin = new ZipInputStream(new ByteArrayInputStream(shapeZip));
        ZipEntry entry;
        Set<Path> files = new TreeSet<>();
        while (null != (entry = zipin.getNextEntry())) {
            Path file = unzipFolder.resolve(entry.getName());
            save(file, zipin.readAllBytes());
            files.add(file);
        }
        return files;
    }

    private void save(Path file, byte[] contents) throws IOException {
        Files.copy(new ByteArrayInputStream(contents), file);
    }

    private SimpleFeatureCollection features() {
        SimpleFeatureType schema = featureType();
        DefaultFeatureCollection col = new DefaultFeatureCollection("col", schema);

        // ~ 2024-05-02 GMT+02
        java.util.Date date = new java.util.Date(1714646315000L);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        col.add(feature(schema, "123", //
                "string val", //
                1000, //
                date, geom("POINT(0 89)")));
        return col;
    }

    private SimpleFeature feature(SimpleFeatureType schema, String id, Object... values) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType());
        for (int i = 0; i < values.length; i++) {
            fb.set(i, values[i]);
        }
        return fb.buildFeature(id);
    }

    private SimpleFeatureType featureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("test");
        builder.setNamespaceURI("http://test");
        builder.setSRS("EPSG:4326");
        builder.add("string", String.class);
        builder.add("int", Integer.class);
        builder.add("date", java.util.Date.class);
        builder.add("pointProperty", Point.class);

        return builder.buildFeatureType();
    }

    private FeatureCollection featureCollection() {
        Collection col = new Collection("col-id", List.of());
        SimpleFeatureCollection features = features();
        GeoToolsFeatureCollection collection = new GeoToolsFeatureCollection(col, features);
        collection.setNumberMatched(1L);
        collection.setNumberReturned(1L);

        return collection;
    }

    @SneakyThrows
    private Geometry geom(String wkt) {
        return new WKTReader().read(wkt);
    }
}

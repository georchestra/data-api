package com.camptocamp.geotools.data.decorate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.util.StringUtils;

import com.camptocamp.opendata.ogc.features.sampledata.SampleData;

class PrefixingDataStoreTest {

    private static SampleData sampleData;
    private DataStore delegate;
    private PrefixingDataStore store;
    private String prefix = "ville_Villeneuve-d'Ascq:";

    @BeforeEach
    void before() {
        delegate = sampleData.get();
        store = new PrefixingDataStore(delegate, () -> prefix);
    }

    @BeforeAll
    static void createSampleData() {
        sampleData = new SampleData();
        sampleData.get();
    }

    @AfterAll
    static void deleteSampleData() throws IOException {
        sampleData.destroy();
    }

    @Test
    void getTypeNames() throws IOException {
        var orig = Stream.of(delegate.getTypeNames()).toList();
        assertThat(orig).isNotEmpty();

        var expected = orig.stream().map(name -> prefix + name).toList();
        var actual = Stream.of(store.getTypeNames()).toList();
        assertThat(actual).isEqualTo(expected);

        prefix = null;
        actual = Stream.of(store.getTypeNames()).toList();
        assertThat(actual).isEqualTo(orig);

        prefix = "";
        actual = Stream.of(store.getTypeNames()).toList();
        assertThat(actual).isEqualTo(orig);
    }

    @Test
    void getNames() throws IOException {
        List<Name> orig = delegate.getNames();
        assertThat(orig).isNotEmpty();

        List<NameImpl> expected = orig.stream().map(name -> prefixed(name)).toList();
        List<Name> actual = store.getNames();
        assertThat(actual).isEqualTo(expected);

        prefix = null;
        actual = store.getNames();
        assertThat(actual).isEqualTo(orig);

        prefix = "";
        actual = store.getNames();
        assertThat(actual).isEqualTo(orig);
    }

    private NameImpl prefixed(Name name) {
        return new NameImpl(prefix + name.getLocalPart());
    }

    @Test
    void getSchemaName() throws IOException {
        List<Name> orig = delegate.getNames();
        List<Name> prefixed = store.getNames();

        for (int i = 0; i < orig.size(); i++) {
            SimpleFeatureType expected = delegate.getSchema(orig.get(i));
            SimpleFeatureType actual = store.getSchema(prefixed.get(i));
            assertSchema(expected, actual);
        }
    }

    @Test
    void getSchemaString() throws IOException {
        String[] orig = delegate.getTypeNames();
        String[] prefixed = store.getTypeNames();

        for (int i = 0; i < orig.length; i++) {
            SimpleFeatureType expected = delegate.getSchema(orig[i]);
            SimpleFeatureType actual = store.getSchema(prefixed[i]);
            assertSchema(expected, actual);
        }
    }

    @Test
    void getFeatureSource() throws IOException {
        String[] orig = delegate.getTypeNames();
        String[] prefixed = store.getTypeNames();

        for (int i = 0; i < orig.length; i++) {
            SimpleFeatureSource expected = delegate.getFeatureSource(orig[i]);
            SimpleFeatureSource actual = store.getFeatureSource(prefixed[i]);
            assertFeatureSource(expected, actual);
        }
    }

    @Test
    void getFeatureSourceName() throws IOException {
        List<Name> orig = delegate.getNames();
        List<Name> prefixed = store.getNames();

        for (int i = 0; i < orig.size(); i++) {
            SimpleFeatureSource expected = delegate.getFeatureSource(orig.get(i));
            SimpleFeatureSource actual = store.getFeatureSource(prefixed.get(i));
            assertFeatureSource(expected, actual);
        }
    }

    @Test
    void getFeatureReader() throws Exception {
        String[] orig = delegate.getTypeNames();
        String[] prefixed = store.getTypeNames();

        Transaction transaction = Transaction.AUTO_COMMIT;
        for (int i = 0; i < orig.length; i++) {
            var expected = delegate.getFeatureReader(new Query(orig[i]), transaction);
            var actual = store.getFeatureReader(new Query(prefixed[i]), transaction);
            assertFeatureReader(expected, actual);
        }
    }

    @Test
    void getFeatureWriter() {
        assertUnsupported(() -> store.getFeatureWriter("test", mock(Transaction.class)));
        assertUnsupported(() -> store.getFeatureWriter("test", mock(Filter.class), mock(Transaction.class)));
    }

    @Test
    void getFeatureWriterAppend() {
        assertUnsupported(() -> store.getFeatureWriterAppend("test", mock(Transaction.class)));
    }

    @Test
    void createSchema() {
        assertUnsupported(() -> store.createSchema(mock(SimpleFeatureType.class)));
    }

    @Test
    void updateSchema() {
        assertUnsupported(() -> store.updateSchema("test", mock(SimpleFeatureType.class)));
        assertUnsupported(() -> store.updateSchema(new NameImpl("test"), mock(SimpleFeatureType.class)));
    }

    @Test
    void removeSchemaString() {
        assertUnsupported(() -> store.removeSchema("test"));
    }

    @Test
    void removeSchemaName() {
        assertUnsupported(() -> store.removeSchema(new NameImpl("test")));
    }

    @SuppressWarnings("unchecked")
    private void assertFeatureSource(SimpleFeatureSource orig, SimpleFeatureSource prefixed) throws IOException {
        assertSchema(orig.getSchema(), prefixed.getSchema());

        Query prefixedQuery = new Query(prefixed.getName().getLocalPart());
        Query origQuery = new Query(orig.getName().getLocalPart());

        assertThat(prefixed.getDataStore()).isSameAs(store);
        assertThat(prefixed.getName()).isEqualTo(prefixed(orig.getName()));
        assertThat(prefixed.getBounds()).isEqualTo(orig.getBounds());
        assertThat(prefixed.getBounds(prefixedQuery)).isEqualTo(orig.getBounds(origQuery));
        assertThat(prefixed.getCount(prefixedQuery)).isEqualTo(orig.getCount(origQuery));

        assertFeatureCollection(orig.getFeatures(), prefixed.getFeatures());
        assertFeatureCollection(orig.getFeatures(origQuery), prefixed.getFeatures(prefixedQuery));
        assertFeatureCollection(orig.getFeatures(Filter.INCLUDE), prefixed.getFeatures(Filter.INCLUDE));
    }

    private void assertFeatureCollection(SimpleFeatureCollection orig, SimpleFeatureCollection prefixed) {
        assertSchema(orig.getSchema(), prefixed.getSchema());
        SimpleFeatureIterator origFeatures = orig.features();
        SimpleFeatureIterator prefixedFeatures = prefixed.features();
        while (origFeatures.hasNext()) {
            assertThat(prefixedFeatures.hasNext()).isTrue();
            assertFeature(origFeatures.next(), prefixedFeatures.next());
        }
        assertThat(prefixedFeatures.hasNext()).isFalse();
    }

    private void assertFeatureReader(FeatureReader<SimpleFeatureType, SimpleFeature> orig,
            FeatureReader<SimpleFeatureType, SimpleFeature> prefixed) throws Exception {

        assertSchema(orig.getFeatureType(), prefixed.getFeatureType());

        while (orig.hasNext()) {
            assertThat(prefixed.hasNext()).isTrue();
            SimpleFeature origFeature = orig.next();
            SimpleFeature prefixedFeature = prefixed.next();
            assertFeature(origFeature, prefixedFeature);
        }
        assertThat(prefixed.hasNext()).isFalse();
    }

    private void assertFeature(SimpleFeature orig, SimpleFeature prefixed) {
        assertSchema(orig.getFeatureType(), prefixed.getFeatureType());

        assertThat(prefixed.getID()).isEqualTo(orig.getID());

        SimpleFeature unprefixed = SimpleFeatureBuilder.build(orig.getFeatureType(), prefixed.getAttributes(),
                prefixed.getID());

        assertThat(unprefixed).isEqualTo(orig);
    }

    private void assertSchema(SimpleFeatureType orig, SimpleFeatureType prefixed) {
        if (StringUtils.hasText(prefix)) {
            assertThat(prefixed.getTypeName()).isEqualTo("%s%s".formatted(prefix, orig.getTypeName()));
        } else {
            assertThat(prefixed.getTypeName()).isEqualTo(orig.getTypeName());
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(prefixed);
        builder.setName(orig.getTypeName());
        SimpleFeatureType actual = builder.buildFeatureType();
        assertThat(actual).isEqualTo(orig);
    }

    private void assertUnsupported(Executable action) {
        var ex = assertThrows(UnsupportedOperationException.class, action);
        assertThat(ex.getMessage()).contains("read only");
    }

}

package com.camptocamp.opendata.ogc.features.http.codec.shp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.MimeType;

import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;

/**
 * Shapefile zip encoder for {@link FeatureCollection}.
 * <p>
 * The media type shall be used for Esri shapefiles. The file is a zip archive
 * that contains at least the shp, shx and dbf files.
 * <p>
 * See <a href=
 * "https://inspire.ec.europa.eu/media-types/application/x-shapefile">x-shapefile</a>.
 */
public class ShapefileFeatureCollectionHttpMessageConverter
        extends AbstractGenericHttpMessageConverter<FeatureCollection> {

    private static final MimeType MIME_TYPE = MimeTypes.SHAPEFILE.getMimeType();
    private static final MediaType MEDIA_TYPE = new MediaType(MIME_TYPE);

    public ShapefileFeatureCollectionHttpMessageConverter() {
        super(MEDIA_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    protected @Override boolean supports(Class<?> clazz) {
        return FeatureCollection.class.isAssignableFrom(clazz);
    }

    /**
     * {@inheritDoc}
     */
    protected @Override MediaType getDefaultContentType(FeatureCollection message) {
        return MEDIA_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    protected @Override void writeInternal(FeatureCollection message, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        Path tmpDir = createTmpDir();
        try {
            Path shp = createShapefile(tmpDir, message);
            Path shpZip = createShapeZipfile(shp);
            writeContent(new FileSystemResource(shpZip), outputMessage);
        } finally {
            FileSystemUtils.deleteRecursively(tmpDir);
        }
    }

    private Path createShapeZipfile(Path shp) throws IOException {
        String baseName = FilenameUtils.getBaseName(shp.getFileName().toString());
        Path dbf = shp.getParent().resolve(baseName + ".dbf");
        Path shx = shp.getParent().resolve(baseName + ".shx");
        Path prj = shp.getParent().resolve(baseName + ".prj");

        Path zip = shp.getParent().resolve(baseName + ".zip");
        try (OutputStream out = new FileOutputStream(zip.toFile());
                ZipOutputStream zout = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            zipEncode(zout, shp);
            zipEncode(zout, dbf);
            zipEncode(zout, shx);
            zipEncode(zout, prj);

            zout.finish();
        }
        return zip;
    }

    private void zipEncode(ZipOutputStream zout, Path file) throws IOException {
        if (Files.exists(file)) {
            ZipEntry e = new ZipEntry(file.getFileName().toString());
            e.setSize(Files.size(file));
            zout.putNextEntry(e);
            Files.copy(file, zout);
            zout.closeEntry();
        }
    }

    private Path createShapefile(Path dir, FeatureCollection message) throws IOException {
        final SimpleFeatureCollection origContents = message.getOriginalContents().orElseThrow();
        if (null == origContents.getSchema().getGeometryDescriptor()) {
            throw new IllegalArgumentException(
                    "Collection %s does not have a geometry attribute, can't encode as Shapefile"
                            .formatted(origContents.getSchema().getTypeName()));
        }

        final SimpleFeatureType featureType = resolveFeatureType(message);
        final String typeName = featureType.getTypeName();
        final Path shp = dir.resolve(typeName + ".shp");
        ShapefileDataStore ds = createDataStore(shp);
        try (SimpleFeatureIterator orig = origContents.features()) {
            ds.createSchema(featureType);
            try (FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = ds
                    .getFeatureWriterAppend(Transaction.AUTO_COMMIT)) {

                while (orig.hasNext()) {
                    SimpleFeature from = orig.next();
                    SimpleFeature to = featureWriter.next();
                    to.setAttributes(from.getAttributes());
                    featureWriter.write();
                }
            }
        } finally {
            ds.dispose();
        }
        return shp;
    }

    private ShapefileDataStore createDataStore(Path shp) throws MalformedURLException {
        // avoid searching for other extensions when missing.
        boolean skipScan = true;
        ShapefileDataStore ds = new ShapefileDataStore(shp.toUri().toURL(), skipScan);
        ds.setCharset(StandardCharsets.UTF_8);
        ds.setFidIndexed(false);
        ds.setIndexed(false);
        ds.setIndexCreationEnabled(false);
        return ds;
    }

    private SimpleFeatureType resolveFeatureType(FeatureCollection message) {
        SimpleFeatureCollection orig = message.getOriginalContents().orElseThrow();
        return orig.getSchema();
    }

    private Path createTmpDir() throws IOException {
        return Files.createTempDirectory("ogc-features-shapefile-format");
    }

    protected void writeContent(Resource resource, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        OutputStream out = outputMessage.getBody();

        try (InputStream in = resource.getInputStream()) {
            in.transferTo(out);
        }

        out.flush();
    }

    protected @Override FeatureCollection readInternal(Class<? extends FeatureCollection> clazz,
            HttpInputMessage inputMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureCollection read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) {
        throw new UnsupportedOperationException();
    }

}

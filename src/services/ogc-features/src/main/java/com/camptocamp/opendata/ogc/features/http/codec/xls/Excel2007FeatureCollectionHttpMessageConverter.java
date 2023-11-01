package com.camptocamp.opendata.ogc.features.http.codec.xls;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.MimeType;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.model.SimpleProperty;
import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.http.codec.xls.StreamingWorkbookWriter.StreamingRow;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.google.common.collect.Streams;

/**
 */
public class Excel2007FeatureCollectionHttpMessageConverter
        extends AbstractGenericHttpMessageConverter<FeatureCollection> {

    private static final MimeType MIME_TYPE = MimeTypes.OOXML.getMimeType();
    private static final MediaType MEDIA_TYPE = new MediaType(MIME_TYPE);

    public Excel2007FeatureCollectionHttpMessageConverter() {
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

        // Streaming version of XSSFWorkbook implementing the "BigGridDemo" strategy.
        // SXSSFWorkbook workbook = new SXSSFWorkbook();
        StreamingWorkbookWriter writer = new StreamingWorkbookWriter(outputMessage.getBody());
        try {
            addHeader(writer, message.getOriginalContents());
            try (Stream<GeodataRecord> features = message.getFeatures()) {
                features.forEach(rec -> {
                    StreamingRow row = writer.newRow();
                    row.addColumnValue(rec.getId());
                    rec.getProperties().stream().map(SimpleProperty::getValue).forEach(row::addColumnValue);
                    if (null != rec.getGeometry()) {
                        row.addColumnValue(rec.getGeometry().getValue());
                    }
                    row.end();
                });
            }
        } finally {
            writer.finish();
        }
    }

    private void addHeader(StreamingWorkbookWriter writer, Optional<SimpleFeatureCollection> originalContents) {
        if (originalContents.isPresent()) {

            SimpleFeatureType schema = originalContents.orElseThrow().getSchema();

            // default geometry is written as the last column
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            String geomName = geometryDescriptor == null ? null : geometryDescriptor.getLocalName();
            var colNames = Streams.concat(//
                    Stream.of("FID"), schema.getAttributeDescriptors().stream().map(AttributeDescriptor::getLocalName))
                    .filter(name -> !name.equals(geomName));
            if (null != geomName)
                colNames = Streams.concat(colNames, Stream.of(geomName));

            StreamingRow header = writer.newRow();
            colNames.forEach(header::addColumnValue);
            header.end();
        }
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

package com.camptocamp.opendata.ogc.features.http.codec.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.geotools.util.Converters;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.MimeType;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.model.GeometryProperty;
import com.camptocamp.opendata.model.SimpleProperty;
import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.opencsv.CSVWriter;

/**
 * CSV encoder for {@link FeatureCollection} using
 * <a href="https://opencsv.sourceforge.net/">OpenCSV</a>.
 * <p>
 * See <a href="https://www.iana.org/assignments/media-types/text/csv">RFC
 * 4180</a>. As per section 4.1.1. of RFC 2046 [RFC2046], this media type uses
 * CRLF to denote line breaks
 * <p>
 * Supported Mime-Type parameters:
 * <ul>
 * <li>charset
 * <li>header: Valid values are "present" or "absent". Defaults to "present"
 * </li>
 */
public class CsvFeatureCollectionHttpMessageConverter extends AbstractGenericHttpMessageConverter<FeatureCollection> {

    private static final MimeType MIME_TYPE = MimeTypes.CSV.getMimeType();
    private static final MediaType MEDIA_TYPE = new MediaType(MIME_TYPE);

    static final String CHARSET_KEY = "charset";
    static final String CHARSET_VALUE = "UTF-8";

    static final String HEADER_KEY = "header";
    static final String HEADER_VALUE = "present";

    private static final String CRLF = "\r\n";

    private char separator = ',';
    private char quotechar = '"';
    private char escapechar = '\\';

    /**
     * As per section 4.1.1. of RFC 2046 [RFC2046], this media type uses CRLF to
     * denote line breaks
     */
    private String lineSeparator = CRLF;
    private boolean quoteAllFields = false;

    public CsvFeatureCollectionHttpMessageConverter() {
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

    protected @Override void writeInternal(FeatureCollection message, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        OutputStream body = outputMessage.getBody();
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(body), separator, quotechar, escapechar, lineSeparator);

        AtomicBoolean headerWritten = new AtomicBoolean();
        message.getFeatures().forEach(rec -> {
            encodeValue(rec, writer, headerWritten);
        });
        writer.flush();
        body.flush();
    }

    private void encodeValue(GeodataRecord rec, CSVWriter writer, AtomicBoolean headerWritten) {
        if (headerWritten.compareAndSet(false, true)) {
            writeHeader(rec, writer);
        }
        String[] nextLine = encode(rec);
        writer.writeNext(nextLine, quoteAllFields);
    }

    private void writeHeader(GeodataRecord rec, CSVWriter writer) {
        Stream<String> names = rec.getProperties().stream().map(SimpleProperty::getName);
        if (null != rec.getGeometry()) {
            names = Stream.concat(names, Stream.of(rec.getGeometry().getName()));
        }
        writer.writeNext(names.toArray(String[]::new));
    }

    private String[] encode(GeodataRecord rec) {
        GeometryProperty geometry = rec.getGeometry();
        List<? extends SimpleProperty<?>> properties = rec.getProperties();
        String[] values = new String[properties.size() + (geometry == null ? 0 : 1)];
        for (int i = 0; i < properties.size(); i++) {
            SimpleProperty<?> p = properties.get(i);
            String v = encode(p);
            values[i] = v;
        }
        if (null != geometry) {
            values[values.length - 1] = encode(geometry);
        }
        return values;
    }

    private String encode(SimpleProperty<?> p) {
        return Converters.convert(p.getValue(), String.class);
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

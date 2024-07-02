package com.camptocamp.opendata.ogc.features.http.codec.json;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Simpler, geometry-less, JSON encoder for {@link FeatureCollection} and
 * {@link GeodataRecord}.
 * 
 * <p>
 * Sample output:
 * 
 * <pre>
 * <code>
 * {
 *     "numberMatched":16,
 *     "numberReturned":2,
 *     "records":[
 *         {
 *             "@id":"1",
 *             "city":" Trento",
 *            "number":140,
 *             "year":2002
 *         },
 *         ...
 *      ],
 *      "links":[
 *          {
 *              "href":"http://localhost:8080/ogcapi/collections/locations/items?f=json&offset=0&limit=2",
 *              "rel":"self",
 *              "type":"application/json",
 *              "title":"This document"
 *          },
 *          ...
 *      ]
 * }                  
 * </code>
 * </pre>
 * 
 */
public class SimpleJsonFeatureCollectionHttpMessageConverter
        extends AbstractGenericHttpMessageConverter<FeatureCollection> {

    private static final MimeType MIME_TYPE = MimeTypes.JSON.getMimeType();
    private static final MediaType MEDIA_TYPE = new MediaType(MIME_TYPE);

    private ObjectMapper mapper;

    public SimpleJsonFeatureCollectionHttpMessageConverter() {
        super(MEDIA_TYPE);
        mapper = new ObjectMapper();
        mapper.setDateFormat(new StdDateFormat());
        Jackson2ObjectMapperBuilder.json().configure(mapper);
        mapper.registerModule(new SimpleJsonModule());
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
        mapper.writeValue(body, message);
        body.flush();
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

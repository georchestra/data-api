package com.camptocamp.opendata.ogc.features.http.codec.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.model.GeometryProperty;
import com.camptocamp.opendata.model.SimpleProperty;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("serial")
class SimpleJsonModule extends SimpleModule {

    private static final Version VERSION = new Version(1, 0, 0, null, null, null);

    public SimpleJsonModule() {
        super(SimpleJsonModule.class.getSimpleName(), VERSION);

        addSerializer(new FeatureCollectionSerializer());
        addSerializer(new GeodataRecordSerializer());
    }

    static class FeatureCollectionSerializer extends StdSerializer<FeatureCollection> {

        protected FeatureCollectionSerializer() {
            super(FeatureCollection.class);
        }

        @Override
        public void serializeWithType(FeatureCollection collection, JsonGenerator gen, SerializerProvider serializers,
                TypeSerializer typeSer) throws IOException {

            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(collection, JsonToken.START_OBJECT));

            serializeContent(collection, gen);

            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(FeatureCollection collection, JsonGenerator generator, SerializerProvider serializers)
                throws IOException {

            if (collection == null) {
                generator.writeNull();
                return;
            }
            generator.writeStartObject();
            serializeContent(collection, generator);
            generator.writeEndObject();
        }

        private void serializeContent(FeatureCollection collection, JsonGenerator generator) throws IOException {
            generator.writeNumberField("numberMatched", collection.getNumberMatched());
            generator.writeNumberField("numberReturned", collection.getNumberReturned());

            generator.writeFieldName("records");
            generator.writeStartArray();
            collection.getFeatures().forEach(rec -> write(rec, generator));
            generator.writeEndArray();

            generator.writeFieldName("links");
            generator.writeStartArray();
            collection.getLinks().forEach(link -> write(link, generator));
            generator.writeEndArray();
        }

        private void write(Object obj, JsonGenerator generator) {
            try {
                generator.writeObject(obj);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

    static class GeodataRecordSerializer extends StdSerializer<GeodataRecord> {

        public GeodataRecordSerializer() {
            super(GeodataRecord.class);
        }

        @Override
        public void serializeWithType(GeodataRecord rec, JsonGenerator gen, SerializerProvider serializers,
                TypeSerializer typeSer) throws IOException {

            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(rec, JsonToken.START_OBJECT));

            serializeContent(rec, gen);

            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(GeodataRecord rec, JsonGenerator generator, SerializerProvider serializers)
                throws IOException {

            if (rec == null) {
                generator.writeNull();
                return;
            }
            generator.writeStartObject();
            serializeContent(rec, generator);
            generator.writeEndObject();
        }

        private void serializeContent(GeodataRecord rec, JsonGenerator generator) throws IOException {
            if (null != rec.getId()) {
                generator.writeStringField("@id", rec.getId());
            }
            writeProperties(generator, rec.getProperties());
        }

        private void writeProperties(JsonGenerator generator, List<? extends SimpleProperty<?>> properties)
                throws IOException {
            for (SimpleProperty<?> p : properties) {
                if (!(p instanceof GeometryProperty)) {
                    generator.writeObjectField(p.getName(), p.getValue());
                }
            }
        }

    }

}

package com.camptocamp.opendata.ogc.features.http.codec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeType;

import lombok.Getter;

public enum MimeTypes {

    SHAPEFILE(new MimeType("application", "x-shapefile")) {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
            contentDisposition(collectionId, "shp.zip", headers);
        }
    },
    CSV(new MimeType("text", "csv", StandardCharsets.UTF_8)) {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
            contentDisposition(collectionId, "csv", headers);
        }
    },
    OOXML(new MimeType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
            contentDisposition(collectionId, "xlsx", headers);
        }
    };

    private final @Getter MimeType mimeType;

    private MimeTypes(MimeType type) {
        this.mimeType = type;
    }

    public static Optional<MimeTypes> find(MimeType contentType) {
        return Arrays.stream(values()).filter(m -> m.getMimeType().isCompatibleWith(contentType)).findFirst();
    }

    public abstract void addHeaders(String collectionId, HttpHeaders headers);

    private static void contentDisposition(String collectionId, String fileExtension, HttpHeaders headers) {
        headers.add("Content-Disposition", "attachment; filename=\"%s.%s\";".formatted(collectionId, fileExtension));
    }
}

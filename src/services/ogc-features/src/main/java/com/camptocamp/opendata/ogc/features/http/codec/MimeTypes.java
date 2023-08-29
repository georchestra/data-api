package com.camptocamp.opendata.ogc.features.http.codec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeType;

import lombok.Getter;
import lombok.NonNull;

public enum MimeTypes {

    JSON(new MimeType("application", "json"), "json", "JSON") {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
        }
    },
    GeoJSON(new MimeType("application", "geo+json"), "geojson", "GeoJSON") {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
        }
    },
    SHAPEFILE(new MimeType("application", "x-shapefile"), "shapefile", "Esri Shapefile") {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
            contentDisposition(collectionId, "shp.zip", headers);
        }
    },
    CSV(new MimeType("text", "csv", StandardCharsets.UTF_8), "csv", "Comma Separated Values") {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
            contentDisposition(collectionId, "csv", headers);
        }
    },
    OOXML(new MimeType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "ooxml",
            "Excel 2007 / OOXML") {
        public @Override void addHeaders(String collectionId, HttpHeaders headers) {
            contentDisposition(collectionId, "xlsx", headers);
        }
    };

    private final @Getter @NonNull MimeType mimeType;
    private final @Getter @NonNull String shortName;
    private final @Getter @NonNull String displayName;

    private MimeTypes(MimeType type, String shortName, String displayName) {
        this.mimeType = type;
        this.shortName = shortName;
        this.displayName = displayName;
    }

    public static Optional<MimeTypes> find(@NonNull MimeType contentType) {
        return Arrays.stream(values()).filter(m -> m.getMimeType().isCompatibleWith(contentType)).findFirst();
    }

    public static Optional<MimeTypes> find(@NonNull String mimeType) {
        MimeType contentType = MimeType.valueOf(mimeType);
        return find(contentType);
    }

    public static Optional<MimeTypes> findByShortName(String parameter) {
        return Optional.ofNullable(parameter)
                .flatMap(p -> Arrays.stream(values()).filter(m -> m.getShortName().equals(p)).findFirst());
    }

    public abstract void addHeaders(String collectionId, HttpHeaders headers);

    private static void contentDisposition(String collectionId, String fileExtension, HttpHeaders headers) {
        headers.add("Content-Disposition", "attachment; filename=\"%s.%s\";".formatted(collectionId, fileExtension));
    }
}

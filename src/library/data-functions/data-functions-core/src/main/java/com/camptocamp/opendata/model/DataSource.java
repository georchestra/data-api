package com.camptocamp.opendata.model;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;

@Value
@With
@Builder
@Accessors(chain = true)
public class DataSource {
    private URI uri;
    @Default
    private Charset encoding = StandardCharsets.UTF_8;
    private AuthCredentials auth;

    public static DataSource fromUri(URI dataUri) {
        return DataSource.builder().uri(dataUri).build();
    }
}

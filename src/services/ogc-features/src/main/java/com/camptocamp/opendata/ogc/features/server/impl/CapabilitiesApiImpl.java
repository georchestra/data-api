package com.camptocamp.opendata.ogc.features.server.impl;

import java.util.*;

import com.camptocamp.opendata.ogc.features.model.*;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.CapabilitiesApiDelegate;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CapabilitiesApiImpl implements CapabilitiesApiDelegate {

    private final @NonNull CollectionRepository repository;

    private @Autowired NativeWebRequest req;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.of(req);
    }

    @Override
    public ResponseEntity<LandingPage> getLandingPage() {
        String basePath = ((HttpServletRequest) req.getNativeRequest()).getRequestURL().toString();
        List<Link> links = List.of(
                // String href, String rel, String type, String title
                link(basePath, "self", "application/json", "This document as JSON"),
                link(basePath + "conformance", "conformance", "application/json", "Conformance"),
                link(basePath + "collections", "data", "application/json", "Collections"),
                link(basePath + "../v3/api-docs", "service-desc", "application/vnd.oai.openapi+json;version=3.0",
                        "OpenAPI definition in JSON format"),
                link(basePath + "../swagger-ui/index.html", "service-doc", "text/html",
                        "OpenAPI definition in HTML format"));
        LandingPage ret = new LandingPage(links);
        ret.setTitle("geOrchestra Data API");
        ret.setDescription("data-api provides an API to access datas");
        return ResponseEntity.ok(ret);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Collections> getCollections() {
        List<Collection> collections = repository.getCollections();
        Collections body = createCollections(collections);
        return ResponseEntity.ok(body);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Collection> describeCollection(String collectionId) {
        return repository.findCollection(collectionId).map(this::addLinks).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<ConfClasses> getConformanceDeclaration() {
        ConfClasses conformance = new ConfClasses(List.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/req/oas30",
                "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/json",
                "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",
                "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/oas30",
                "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/landing-page",
                "http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs",
                "http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/collections",
                "http://www.opengis.net/spec/ogcapi-features-5/1.0/conf/schemas",
                "http://www.opengis.net/spec/ogcapi-features-3/1.0/conf/queryables",
                "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson",
                "http://www.opengis.net/spec/ogcapi-features-3/1.0/conf/queryables-query-parameters",
                "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core",
                "http://www.opengis.net/spec/ogcapi-features-5/1.0/req/core-roles-features",
                "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html"));

        return ResponseEntity.ok(conformance);

    }

    private Collections createCollections(List<Collection> collections) {
        Collections cs = new Collections(new ArrayList<>(), collections);
        addLinks(cs);
        return cs;
    }

    private Collections addLinks(Collections collections) {
        NativeWebRequest request = getRequest().orElseThrow();
        HttpServletRequest nativeRequest = (HttpServletRequest) request.getNativeRequest();
        String basePath = nativeRequest.getRequestURL().toString();
        collections.getCollections().forEach(c -> {
            String colBase = UriComponentsBuilder.fromUriString(basePath).pathSegment(c.getId()).build().toString();
            addLinks(c, colBase);
        });
        return collections;
    }

    private Collection addLinks(Collection collection) {
        NativeWebRequest request = getRequest().orElseThrow();
        HttpServletRequest nativeRequest = (HttpServletRequest) request.getNativeRequest();
        return addLinks(collection, nativeRequest.getRequestURL().toString());
    }

    private Collection addLinks(Collection collection, String baseUrl) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        builder.pathSegment("items");

        MimeTypes defFormat = MimeTypes.GeoJSON;
        UriComponents itemsc = builder.replaceQueryParam("f", defFormat.getShortName()).build();
        Link items = link(itemsc.toString(), "items", defFormat.getMimeType().toString(), collection.getId());
        collection.addLinksItem(items);

        Arrays.stream(MimeTypes.values()).forEach(m -> {
            if (m.supportsItemType(collection.getItemType())) {
                String href = builder.replaceQueryParam("f", m.getShortName()).replaceQueryParam("limit", "-1").build()
                        .toString();
                String type = m.getMimeType().toString();
                String title = "Bulk download (%s)".formatted(m.getDisplayName());
                Link link = link(href, "enclosure", type, title);
                collection.addLinksItem(link);
            }
        });
        return collection;
    }

    private Link link(String href, String rel, String type, String title) {
        Link link = new Link(href);
        link.setRel(rel);
        link.setType(type);
        link.setTitle(title);
        return link;
    }

}

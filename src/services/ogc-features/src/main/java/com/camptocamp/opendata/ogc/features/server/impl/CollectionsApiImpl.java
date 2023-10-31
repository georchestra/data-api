package com.camptocamp.opendata.ogc.features.server.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.Collections;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.Link;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiDelegate;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CollectionsApiImpl implements CollectionsApiDelegate {

    private final @NonNull CollectionRepository repository;

    private @Autowired NativeWebRequest req;

    public Optional<NativeWebRequest> getRequest() {
        return Optional.of(req);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<com.camptocamp.opendata.model.GeodataRecord> getFeature(//
            String collectionId, //
            String featureId) {

        Optional<GeodataRecord> rec = repository.getRecord(collectionId, featureId);
        return rec.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<FeatureCollection> getFeatures(//
            String collectionId, //
            Integer limit, //
            List<BigDecimal> bbox, //
            String datetime, //
            String filter, //
            String filterLang, //
            String filterCrs, //
            List<String> sortby) {

        FeaturesQuery fq = FeaturesQuery.of(collectionId).withLimit(limit).withBbox(bbox).withDatetime(datetime)
                .withFilter(filter).withFilterLang(filterLang).withFilterCrs(filterCrs);
        return getFeatures(fq);
    }

    public ResponseEntity<FeatureCollection> getFeatures(@NonNull FeaturesQuery query) {

        DataQuery dataQuery = toDataQuery(query);

        FeatureCollection fc = repository.query(dataQuery);
        HttpHeaders headers = getFeaturesHeaders(query.getCollectionId());
        fc = addLinks(fc, dataQuery);
        return ResponseEntity.status(200).headers(headers).body(fc);
    }

    private HttpHeaders getFeaturesHeaders(String collectionId) {
        HttpHeaders headers = new HttpHeaders();

        getRequest().map(req -> req.getHeader("Accept")).map(MimeType::valueOf).flatMap(MimeTypes::find)
                .ifPresent(m -> m.addHeaders(collectionId, headers));
        return headers;
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
            String colBase = UriComponentsBuilder.fromPath(basePath).pathSegment(c.getId()).build().toString();
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(baseUrl);
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

    private FeatureCollection addLinks(FeatureCollection fc, DataQuery dataQuery) {
        NativeWebRequest request = getRequest().orElseThrow();

        HttpServletRequest nativeRequest = (HttpServletRequest) request.getNativeRequest();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nativeRequest.getRequestURL().toString());
        MimeTypes requestedFormat = MimeTypes.find(request.getHeader("Accept")).orElse(MimeTypes.GeoJSON);

        UriComponents self = builder.query(nativeRequest.getQueryString()).build();
        String mime = requestedFormat.getMimeType().toString();
        fc.getLinks().add(link(self.toString(), "self", mime, "This document"));

        Arrays.stream(MimeTypes.values()).filter(alt -> alt != requestedFormat).forEach(m -> {

            UriComponents alternate = builder.replaceQueryParam("f", m.getShortName()).build();
            fc.getLinks().add(link(alternate.toString(), "alternate", m.getMimeType().toString(),
                    "This document as " + m.getDisplayName()));
        });

        if (fc.getNumberMatched() != null && fc.getNumberReturned() != null
                && (fc.getNumberMatched() > fc.getNumberReturned())) {

            Integer offset = extractOffset(request);
            int limit = dataQuery.getLimit() == null ? 10 : dataQuery.getLimit();
            Integer next = offset == null ? limit : offset + limit;
            // f parameter preempts the "Accept" header
            final String formatParam = request.getParameter("f");
            builder.replaceQueryParam("f",
                    StringUtils.hasText(formatParam) ? formatParam : requestedFormat.getShortName());
            UriComponents nextUri = builder.replaceQueryParam("offset", next.toString())
                    .replaceQueryParam("limit", limit).build();
            fc.getLinks().add(1, link(nextUri.toString(), "next", mime, "Next page"));
        }
        return fc;
    }

    private Integer extractOffset(NativeWebRequest request) {
        String offsetParam = request.getParameter("offset");
        if (null != offsetParam) {
            try {
                return Integer.parseInt(offsetParam);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("offset is invalid: " + offsetParam);
            }
        }
        return null;
    }

    private Link link(String href, String rel, String type, String title) {
        Link link = new Link(href);
        link.setRel(rel);
        link.setType(type);
        link.setTitle(title);
        return link;
    }

    DataQuery toDataQuery(FeaturesQuery query) {

        List<DataQuery.SortBy> sortby = Optional.ofNullable(query.getSortby()).orElse(List.of()).stream()
                .map(this::toSortBy).toList();

        DataQuery q = DataQuery.fromUri(URI.create("index://default"))//
                .withLayerName(query.getCollectionId())//
                .withLimit(query.getLimit())//
                .withOffset(query.getOffset())//
                .withFilter(query.getFilter())//
                .withSortBy(sortby);
        return q;
    }

    private DataQuery.SortBy toSortBy(String s) {
        String propertyName = s;
        boolean ascending = true;
        if (s.startsWith("+")) {
            propertyName = propertyName.substring(1);
        } else if (s.startsWith("-")) {
            ascending = false;
            propertyName = propertyName.substring(1);
        }
        return new DataQuery.SortBy(propertyName, ascending);
    }

}

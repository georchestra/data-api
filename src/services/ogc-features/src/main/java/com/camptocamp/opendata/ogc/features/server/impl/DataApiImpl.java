package com.camptocamp.opendata.ogc.features.server.impl;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.ACCEPT;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.Link;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.DataApiDelegate;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataApiImpl implements DataApiDelegate {

    private final @NonNull CollectionRepository repository;

    private final @NonNull NativeWebRequest webRequest;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.of(webRequest);
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
            List<String> sortby, String crs) {

        NativeWebRequest request = getRequest().orElseThrow();
        Integer offset = extractOffset(request);

        if (limit != null && limit.intValue() < 0) {
            limit = null;
        }
        FeaturesQuery fq = FeaturesQuery.of(collectionId)//
                .withLimit(limit)//
                .withOffset(offset)//
                .withBbox(bbox)//
                .withDatetime(datetime)//
                .withFilter(filter)//
                .withFilterLang(filterLang)//
                .withFilterCrs(filterCrs)//
                .withSortby(sortby).withCrs(crs);
        return getFeatures(fq);
    }

    public ResponseEntity<FeatureCollection> getFeatures(@NonNull FeaturesQuery query) {

        DataQuery dataQuery = toDataQuery(query);

        FeatureCollection fc = addLinks(repository.query(dataQuery), dataQuery);
        HttpHeaders headers = getFeaturesHeaders(query.getCollectionId());
        return ResponseEntity.status(200).headers(headers).body(fc);
    }

    private HttpHeaders getFeaturesHeaders(String collectionId) {
        HttpHeaders headers = new HttpHeaders();

        getRequest().map(req -> Arrays.stream(req.getHeader(ACCEPT).split(",")).findFirst().get())
                .map(MimeType::valueOf).flatMap(MimeTypes::find).ifPresent(m -> m.addHeaders(collectionId, headers));
        return headers;
    }

    private FeatureCollection addLinks(FeatureCollection fc, DataQuery dataQuery) {
        NativeWebRequest request = getRequest().orElseThrow();

        HttpServletRequest nativeRequest = (HttpServletRequest) request.getNativeRequest();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(nativeRequest.getRequestURL().toString());
        MimeTypes requestedFormat = ofNullable(request.getHeader(ACCEPT)).flatMap(MimeTypes::find)
                .orElse(MimeTypes.GEOJSON);

        UriComponents self = builder.query(nativeRequest.getQueryString()).build();
        String mime = requestedFormat.getMimeType().toString();
        fc.getLinks().add(link(self.toString(), "self", mime, "This document"));

        Arrays.stream(MimeTypes.values()).filter(alt -> alt != requestedFormat).forEach(m -> {

            UriComponents alternate = builder.replaceQueryParam("f", m.getShortName()).build();
            fc.getLinks().add(link(alternate.toString(), "alternate", m.getMimeType().toString(),
                    "This document as %s".formatted(m.getDisplayName())));
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
                throw new IllegalArgumentException("offset is invalid: %s".formatted(offsetParam));
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

        List<DataQuery.SortBy> sortby = query.sortBy();

        return DataQuery.fromUri(URI.create("index://default"))//
                .withLayerName(query.getCollectionId())//
                .withLimit(query.getLimit())//
                .withOffset(query.getOffset())//
                .withFilter(query.getFilter())//
                .withSortBy(sortby).withTargetCrs(query.getCrs());
    }

}

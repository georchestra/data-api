package com.camptocamp.opendata.ogc.features.server.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.context.request.NativeWebRequest;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.model.Collection;
import com.camptocamp.opendata.ogc.features.model.Collections;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.Link;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiDelegate;

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
        return repository.findCollection(collectionId).map(ResponseEntity::ok)
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
            String datetime) {

        DataQuery dataQuery = toDataQuery(collectionId, limit, bbox, datetime);

        FeatureCollection fc = repository.query(dataQuery);
        HttpHeaders headers = getFeaturesHeaders(collectionId);
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
        collections.forEach(this::addLinks);
        ArrayList<Link> links = new ArrayList<>();
        Collections cs = new Collections(links, collections);
        addLinks(cs);
        return cs;
    }

    private void addLinks(Collections collections) {
    }

    private void addLinks(Collection collection) {
    }

    private FeatureCollection addLinks(FeatureCollection fc, DataQuery dataQuery) {
//		
//		URI origin = exchange.getRequest().getURI();
//		UriComponents self = UriComponentsBuilder.fromUri(origin).query(null).build();
//		fc.getLinks().add(link("item", self.toString()));
//
//		if (fc.getNumberMatched() != null && fc.getNumberReturned() != null
//				&& (fc.getNumberMatched() > fc.getNumberReturned())) {
//
//			Integer offset = extractOffset(exchange.getRequest());
//			int limit = dataQuery.getLimit() == null ? 10 : dataQuery.getLimit();
//			Integer next = offset == null ? limit : offset + limit;
//			UriComponents nextUri = UriComponentsBuilder.fromUri(origin).replaceQueryParam("offset", next.toString())
//					.replaceQueryParam("limit").build();
//			fc.getLinks().add(link("next", nextUri.toString()));
//		}
        return fc;
    }

//	private Integer extractOffset(ServerHttpRequest request) {
//		String offsetParam = request.getQueryParams().getFirst("offset");
//		if (null != offsetParam) {
//			try {
//				return Integer.parseInt(offsetParam);
//			} catch (NumberFormatException e) {
//				throw new IllegalArgumentException("offset is invalid: " + offsetParam);
//			}
//		}
//		return null;
//	}

    private Link link(String rel, String href) {
        Link link = new Link(href);
        link.setRel(rel);
        return link;
    }

    DataQuery toDataQuery(String collectionId, //
            Integer limit, //
            List<BigDecimal> bbox, //
            String datetime) {

        // query collection id from whatever datasource is defined as index
        DataQuery q = DataQuery.fromUri(URI.create("index://default")).withLayerName(collectionId);
        if (null != limit && limit >= 0) {
            // a negative limit can be used to return the whole dataset
            q = q.withLimit(limit);
        }
        return q;
    }
}

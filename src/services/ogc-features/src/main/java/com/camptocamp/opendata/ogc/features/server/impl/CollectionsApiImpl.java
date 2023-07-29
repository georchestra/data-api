package com.camptocamp.opendata.ogc.features.server.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.ogc.features.model.FeatureCollection;
import com.camptocamp.opendata.ogc.features.model.Link;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiDelegate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CollectionsApiImpl implements CollectionsApiDelegate {

    private final @NonNull CollectionRepository repository;

    @Override
    public Mono<ResponseEntity<FeatureCollection>> getFeatures(//
            String collectionId, //
            Integer limit, //
            List<BigDecimal> bbox, //
            String datetime, //
            ServerWebExchange exchange//
    ) {

        DataQuery dataQuery = toDataQuery(collectionId, limit, bbox, datetime, exchange);
        return Mono.just(dataQuery).map(repository::query).map(fc -> addLinks(fc, dataQuery, exchange))
                .map(ResponseEntity::ok);
    }

    private FeatureCollection addLinks(FeatureCollection fc, DataQuery dataQuery, ServerWebExchange exchange) {
        List<Link> links = new LinkedList<>();
        URI origin = exchange.getRequest().getURI();
        UriComponents self = UriComponentsBuilder.fromUri(origin).query(null).build();
        links.add(link("item", self.toString()));

        if (fc.getNumberMatched() != null && fc.getNumberReturned() != null
                && (fc.getNumberMatched() > fc.getNumberReturned())) {

            Integer offset = extractOffset(exchange.getRequest());
            int limit = dataQuery.getLimit() == null ? 10 : dataQuery.getLimit();
            Integer next = offset == null ? limit : offset + limit;
            UriComponents nextUri = UriComponentsBuilder.fromUri(origin).replaceQueryParam("offset", next.toString())
                    .replaceQueryParam("limit").build();
            links.add(link("next", nextUri.toString()));
        }
        fc.setLinks(links);
        return fc;
    }

    private Integer extractOffset(ServerHttpRequest request) {
        String offsetParam = request.getQueryParams().getFirst("offset");
        if (null != offsetParam) {
            try {
                return Integer.parseInt(offsetParam);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("offset is invalid: " + offsetParam);
            }
        }
        return null;
    }

    private Link link(String rel, String href) {
        Link link = new Link(href);
        link.setRel(rel);
        return link;
    }

    DataQuery toDataQuery(String collectionId, //
            Integer limit, //
            List<BigDecimal> bbox, //
            String datetime, //
            ServerWebExchange exchange) {

        // query collection id from whatever datasource is defined as index
        return DataQuery.fromUri(URI.create("index://default")).withLayerName(collectionId).withLimit(limit);
    }
}

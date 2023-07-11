package com.camptocamp.ogc.features.server.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;

import com.camptocamp.ogc.features.repository.CollectionRepository;
import com.camptocamp.ogc.features.server.api.CollectionsApiDelegate;
import com.camptocamp.ogc.features.server.model.FeatureCollection;
import com.camptocamp.opendata.model.DataQuery;

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
        return Mono.just(dataQuery).map(repository::query).map(ResponseEntity::ok);
    }

    DataQuery toDataQuery(String collectionId, //
            Integer limit, //
            List<BigDecimal> bbox, //
            String datetime, //
            ServerWebExchange exchange) {
    	
    	//query collection id from whatever datasource is defined as index
    	return DataQuery.fromUri(URI.create("index://" + collectionId));
    }
}

package com.camptocamp.opendata.indexing.app;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.camptocamp.opendata.indexing.processor.Processors;
import com.camptocamp.opendata.indexing.sink.Consumers;
import com.camptocamp.opendata.model.AuthCredentials;
import com.camptocamp.opendata.model.AuthCredentials.AuthType;
import com.camptocamp.opendata.model.DataQuery;
import com.camptocamp.opendata.model.DataSource;
import com.camptocamp.opendata.model.GeodataRecord;
import com.camptocamp.opendata.producer.Producers;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
@SpringBootApplication
public class IndexingApp {

    private @Autowired Producers producers;
    private @Autowired Processors processors;
    private @Autowired Consumers consumers;

    /**
     * Application launcher.
     */
    public static void main(String[] args) {
        try {
            SpringApplication.run(IndexingApp.class, args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Bean
    Supplier<DataQuery> sampleQuery() {

        AuthCredentials auth = AuthCredentials.builder().userName("user").password("secret").type(AuthType.basic)
                .build();
        DataSource source = DataSource.builder().uri(URI.create("http://ows.example.com/wfs?request=GetCapabilities"))
                .encoding(StandardCharsets.UTF_8).auth(auth).build();

        return () -> DataQuery.builder().source(source).layerName("topp:states").build();
    }

    @Bean
    Function<Flux<URI>, Flux<GeodataRecord>> indexAll() {
        // Consumer<GeodataRecord> c;
        Function<Flux<URI>, Flux<GeodataRecord>> source = readAll().andThen(toWgs84());
        return source;
    }

    @Bean
    Consumer<Flux<GeodataRecord>> index() {
        return records -> {
            log.info("index()");
            consumers.index(records.publishOn(Schedulers.parallel()));
        };
    }

    /**
     * A function that takes a dataset URI and returns a flux of
     * {@link GeodataRecord}.
     * 
     * <p>
     * Maps to HTTP as: {@code POST /read} with an {@link URI} as request body.
     * 
     * @return a function that takes a dataset URI and returns a flux of
     *         {@link GeodataRecord}
     */
    @Bean
    Function<Flux<URI>, Flux<GeodataRecord>> readAll() {
        return uris -> uris.flatMap(uri -> toFlux(producers.read(DataQuery.fromUri(uri))));
    }

    @Bean
    Function<Flux<DataQuery>, Flux<GeodataRecord>> read() {
        return queries -> queries.flatMap(query -> {
            log.info("read() {}", query);
            return toFlux(producers.read(query));
        });
    }

    @Bean
    Function<Flux<GeodataRecord>, Flux<GeodataRecord>> toWgs84() {
        return flux -> toFlux(() -> {
            log.info("toWgs84()");
            Stream<GeodataRecord> in = flux.toStream();
            Stream<GeodataRecord> o = processors.toWgs84().apply(in);
            return o;
        });
    }

    /**
     * Adapts a {@link Stream} to a {@link Flux}, note that
     * {@link Flux#fromStream(Stream)} already makes sure {@link Stream#close()} is
     * called when the stream is fully consumed.
     */
    private <T> Flux<T> toFlux(Supplier<Stream<? extends T>> stream) {
        return Flux.fromStream(stream)//
                // subscribe on Schedulers.boundedElastic() to avoid
                // "java.lang.IllegalStateException: Iterating over a toIterable() / toStream()
                // is blocking, which is not supported in thread reactor-http-epoll-XXX"
                .subscribeOn(Schedulers.boundedElastic());
    }
}

package com.camptocamp.opendata.ogc.features.autoconfigure.api;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.camptocamp.opendata.ogc.features.server.api.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.camptocamp.opendata.ogc.features.autoconfigure.geotools.PostgisBackendAutoConfiguration;
import com.camptocamp.opendata.ogc.features.autoconfigure.geotools.SampleDataBackendAutoConfiguration;
import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.http.codec.csv.CsvFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.json.SimpleJsonFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.shp.ShapefileFeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.http.codec.xls.Excel2007FeatureCollectionHttpMessageConverter;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.config.HomeController;
import com.camptocamp.opendata.ogc.features.server.config.SpringDocConfiguration;
import com.camptocamp.opendata.ogc.features.server.impl.CapabilitiesApiImpl;
import com.camptocamp.opendata.ogc.features.server.impl.DataApiImpl;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * {@link AutoConfiguration @AutoConfiguration} to set up the OGC Features API
 * <p>
 * Requires the following beans:
 * <ul>
 * <li>{@link CollectionRepository}
 * </ul>
 *
 * @see PostgisBackendAutoConfiguration
 * @see SampleDataBackendAutoConfiguration
 */
@AutoConfiguration
@Import(SpringDocConfiguration.class)
public class ApiAutoConfiguration implements WebMvcConfigurer {

    @Bean
    HomeController homeController() {
        return new HomeController();
    }

    @Bean
    CapabilitiesApiController capabilitiesApi(CapabilitiesApiDelegate delegate) {
        return new CapabilitiesApiController(delegate);
    }

    @Bean
    DataApiController dataApiController(DataApiDelegate delegate) {
        return new DataApiController(delegate);
    }

    @Bean
    CapabilitiesApiDelegate capabilitiesApiDelegate(CollectionRepository repo) {
        return new CapabilitiesApiImpl(repo);
    }

    @Bean
    DataApiDelegate dataApiDelegate(CollectionRepository repo) {
        return new DataApiImpl(repo);
    }

    /**
     * {@inheritDoc}
     *
     * @see CsvFeatureCollectionHttpMessageConverter
     * @see ShapefileFeatureCollectionHttpMessageConverter
     * @see Excel2007FeatureCollectionHttpMessageConverter
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(simpleJsonFeatureCollectionHttpMessageConverter());
        converters.add(csvFeatureCollectionHttpMessageConverter());
        converters.add(shapefileFeatureCollectionHttpMessageConverter());
        converters.add(excel2007FeatureCollectionHttpMessageConverter());
    }

    @Bean
    SimpleJsonFeatureCollectionHttpMessageConverter simpleJsonFeatureCollectionHttpMessageConverter() {
        return new SimpleJsonFeatureCollectionHttpMessageConverter();
    }

    @Bean
    Excel2007FeatureCollectionHttpMessageConverter excel2007FeatureCollectionHttpMessageConverter() {
        return new Excel2007FeatureCollectionHttpMessageConverter();
    }

    @Bean
    ShapefileFeatureCollectionHttpMessageConverter shapefileFeatureCollectionHttpMessageConverter() {
        return new ShapefileFeatureCollectionHttpMessageConverter();
    }

    @Bean
    CsvFeatureCollectionHttpMessageConverter csvFeatureCollectionHttpMessageConverter() {
        return new CsvFeatureCollectionHttpMessageConverter();
    }

    /**
     * Filter that replaces the {@literal Accept} request header by the MimeType of
     * a matching collection items {@link MimeTypes supported format}, if the
     * {@literal f} query parameter matches a {@link MimeTypes#getShortName() short
     * name} supported output format.
     * <p>
     * E.g.: {@literal GET /ogcapi/collections/{collectionId}/items?f=shapefile}
     * matches {@link MimeTypes#SHAPEFILE} and the Accept header is set to
     * {@literal application/x-shapefile}
     */
    @Bean
    FilterRegistrationBean<CollectionItemsFormatParamContentTypeFilter> forceItemsContentTypeFilter() {
        FilterRegistrationBean<CollectionItemsFormatParamContentTypeFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new CollectionItemsFormatParamContentTypeFilter());
        registrationBean.addUrlPatterns("/ogcapi/collections/*");
        registrationBean.setOrder(-1);

        return registrationBean;
    }

    static class CollectionItemsFormatParamContentTypeFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            final HttpServletRequest httpreq = (HttpServletRequest) request;
            final String requestURI = httpreq.getRequestURI();
            final boolean isItemsRequest = requestURI.endsWith("/items");
            if (!isItemsRequest) {
                chain.doFilter(request, response);
                return;
            }

            final String formatParam = request.getParameter("f");
            final MimeTypes formatOverride;
            if (formatParam != null) {
                formatOverride = MimeTypes.findByShortName(formatParam).orElse(null);
            } else {
                String accept = httpreq.getHeader("Accept");
                List<String> list = accept == null ? List.of() : Arrays.asList(accept.split(","));
                boolean anyMatch = list.stream().filter(t -> !t.startsWith("*/*"))
                        .anyMatch(reqType -> MimeTypes.find(reqType).isPresent());
                formatOverride = anyMatch ? null : MimeTypes.GeoJSON;
            }

            if (formatOverride != null) {
                HttpServletRequestWrapper w = new HttpServletRequestWrapper(httpreq) {
                    final String formatMime = formatOverride.getMimeType().toString();

                    public @Override Enumeration<String> getHeaders(String name) {
                        if ("Accept".equalsIgnoreCase(name)) {
                            return Collections.enumeration(List.of(formatMime));
                        }
                        return super.getHeaders(name);
                    }

                    public @Override String getHeader(String name) {
                        if ("Accept".equalsIgnoreCase(name)) {
                            return formatMime;
                        }
                        return super.getHeader(name);
                    }
                };
                request = w;
            }
            chain.doFilter(request, response);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedMethods("*");
    }
}

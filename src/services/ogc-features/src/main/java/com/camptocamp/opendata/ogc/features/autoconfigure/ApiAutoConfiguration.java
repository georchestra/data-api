package com.camptocamp.opendata.ogc.features.autoconfigure;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.camptocamp.opendata.ogc.features.http.codec.MimeTypes;
import com.camptocamp.opendata.ogc.features.repository.CollectionRepository;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiController;
import com.camptocamp.opendata.ogc.features.server.api.CollectionsApiDelegate;
import com.camptocamp.opendata.ogc.features.server.config.HomeController;
import com.camptocamp.opendata.ogc.features.server.config.SpringDocConfiguration;
import com.camptocamp.opendata.ogc.features.server.impl.CollectionsApiImpl;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

@AutoConfiguration
@Import(SpringDocConfiguration.class)
public class ApiAutoConfiguration {

    @Bean
    HomeController homeController() {
        return new HomeController();
    }

    @Bean
    CollectionsApiDelegate collectionsApiDelegate(CollectionRepository repo) {
        return new CollectionsApiImpl(repo);
    }

    @Bean
    CollectionsApiController collectionsApiController(CollectionsApiDelegate delegate) {
        return new CollectionsApiController(delegate);
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
}

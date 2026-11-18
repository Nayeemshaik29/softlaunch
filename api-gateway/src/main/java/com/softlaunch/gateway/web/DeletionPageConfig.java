package com.softlaunch.gateway.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebFilter;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * The public "delete my account" web page (Play Store rule), served by the gateway itself:
 * same address as the API, so the page's fetch() calls need no CORS setup.
 * Files: src/main/resources/static/delete-account/ (index.html, app.js, style.css).
 */
@Configuration(proxyBeanMethods = false)
class DeletionPageConfig {

    static final String PATH = "/delete-account";

    /**
     * Content-Security-Policy: the browser runs/loads ONLY our own files and talks ONLY to our own API.
     * Even if someone found a way to inject HTML into the page, injected scripts would not run.
     */
    private static final String CSP = String.join("; ",
            "default-src 'none'",
            "script-src 'self'",
            "style-src 'self'",
            "connect-src 'self'",
            "img-src 'self'",
            "form-action 'none'",
            "base-uri 'none'",
            "frame-ancestors 'none'");   // can't be shown inside another site's frame (clickjacking)

    private final Resource page = new ClassPathResource("static/delete-account/index.html");

    @Bean
    RouterFunction<ServerResponse> deletionPage() {
        return RouterFunctions.route(GET(PATH),
                request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(page));
    }

    @Bean
    WebFilter deletionPageHeaders() {
        return (exchange, chain) -> {
            if (exchange.getRequest().getPath().value().startsWith(PATH)) {
                exchange.getResponse().beforeCommit(() -> {
                    HttpHeaders h = exchange.getResponse().getHeaders();
                    h.set("Content-Security-Policy", CSP);
                    h.set("Referrer-Policy", "no-referrer");
                    h.set("X-Content-Type-Options", "nosniff");
                    h.set("Cache-Control", "no-store");
                    return reactor.core.publisher.Mono.empty();
                });
            }
            return chain.filter(exchange);
        };
    }
}

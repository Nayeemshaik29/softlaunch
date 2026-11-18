package com.softlaunch.gateway.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;


/**
 * Public web pages the Play Store asks for, served by the gateway itself (same address as the API,
 * so the deletion page's fetch() calls need no CORS setup). Files: src/main/resources/static/.
 *   /delete-account  account deletion without the app (static/delete-account/)
 *   /privacy         privacy policy (static/privacy/), written from docs/DATA_INVENTORY.md
 *   /pages/...       shared stylesheet
 */
@Configuration(proxyBeanMethods = false)
class PublicPagesConfig {

    private static final Map<String, String> PAGES = Map.of(
            "/delete-account", "static/delete-account/index.html",
            "/privacy", "static/privacy/index.html");

    private static final List<String> PREFIXES = List.of("/delete-account", "/privacy", "/pages/");

    /**
     * Content-Security-Policy: the browser runs/loads ONLY our own files and talks ONLY to our own API.
     * Even if someone found a way to inject HTML into a page, injected scripts would not run.
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

    @Bean
    RouterFunction<ServerResponse> publicPages() {
        RouterFunctions.Builder routes = RouterFunctions.route();
        PAGES.forEach((path, file) -> {
            ClassPathResource page = new ClassPathResource(file);
            routes.GET(path, request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(page));
        });
        return routes.build();
    }

    @Bean
    WebFilter publicPageHeaders() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            if (PREFIXES.stream().anyMatch(path::startsWith)) {
                exchange.getResponse().beforeCommit(() -> {
                    HttpHeaders h = exchange.getResponse().getHeaders();
                    h.set("Content-Security-Policy", CSP);
                    h.set("Referrer-Policy", "no-referrer");
                    h.set("X-Content-Type-Options", "nosniff");
                    // the policy may be cached briefly; the deletion page never (it handles one-time links)
                    h.set("Cache-Control", path.startsWith("/delete-account") ? "no-store" : "public, max-age=300");
                    return Mono.empty();
                });
            }
            return chain.filter(exchange);
        };
    }
}

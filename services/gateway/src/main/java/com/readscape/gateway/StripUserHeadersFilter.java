package com.readscape.gateway;

import java.util.List;
import java.util.Locale;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Drops client-supplied {@code X-User-*} headers so identity can never be spoofed towards a service. */
@Component
public class StripUserHeadersFilter implements GlobalFilter, Ordered {

    private static final String PREFIX = "x-user-";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        boolean present = request.getHeaders().headerNames().stream()
                .anyMatch(n -> n.toLowerCase(Locale.ROOT).startsWith(PREFIX));
        if (!present) {
            return chain.filter(exchange);
        }
        ServerHttpRequest stripped = request.mutate()
                .headers(h -> List.copyOf(h.headerNames()).stream()
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(PREFIX))
                        .forEach(h::remove))
                .build();
        return chain.filter(exchange.mutate().request(stripped).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

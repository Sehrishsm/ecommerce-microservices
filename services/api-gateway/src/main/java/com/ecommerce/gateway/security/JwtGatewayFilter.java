package com.ecommerce.gateway.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey signingKey;

    public JwtGatewayFilter(
            @Value("${app.jwt.secret}") String secret) {

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Allow registration, login, and the public ping endpoint.
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration() == null
                    || !claims.getExpiration().after(new Date())) {
                return unauthorized(exchange);
            }

            // Preserve the original Authorization header.
            return chain.filter(exchange);

        } catch (JwtException | IllegalArgumentException exception) {
            return unauthorized(exchange);
        }
    }

    private boolean isPublicEndpoint(String path, String method) {

        if ("POST".equals(method)
                && ("/api/auth/register".equals(path)
                        || "/api/auth/login".equals(path))) {
            return true;
        }

        return "GET".equals(method)
                && "/api/auth/ping".equals(path);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
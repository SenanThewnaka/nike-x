package com.zentora.nike_x.middleware;

import com.zentora.nike_x.util.Env;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
public class CorsAndSecurityFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        // Dynamic frontend URL checking. Fallback to common dev ports.
        String origin = requestContext.getHeaderString("Origin");
        if (origin != null && (origin.equals("http://localhost:5173") || origin.equals("http://localhost:3000")
                || origin.equals(Env.get("APP_URL")))) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
        } else {
            // Default restriction if no specific match
            responseContext.getHeaders().add("Access-Control-Allow-Origin",
                    Env.get("APP_URL") != null ? Env.get("APP_URL") : "http://localhost:8080");
        }

        responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");

        // Security Headers (OWASP Recommended)
        responseContext.getHeaders().add("X-Content-Type-Options", "nosniff");
        responseContext.getHeaders().add("X-Frame-Options", "DENY");
        responseContext.getHeaders().add("X-XSS-Protection", "1; mode=block");
        responseContext.getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        // We only allow self logic to circumvent CSRF easily while relying entirely on
        // pure API requests.
        responseContext.getHeaders().add("Content-Security-Policy", "default-src 'self'");
    }
}

package com.zentora.nike_x.middleware;

import com.zentora.nike_x.annotation.IsUser;
import jakarta.annotation.Priority;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URI;

@Provider
@Priority(Priorities.AUTHENTICATION)
@IsUser
public class AuthFilter implements ContainerRequestFilter {
    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        HttpSession httpSession = request.getSession(false);
        System.out.println(request.getContextPath());
        if (httpSession == null || httpSession.getAttribute("user") == null) {
            containerRequestContext
                    .abortWith(Response.status(Response.Status.TEMPORARY_REDIRECT)
                            .location(URI.create(request.getContextPath() + "/sign-in.html")).build());
        }

    }
}

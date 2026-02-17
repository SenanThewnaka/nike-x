package com.zentora.nike_x.middleware;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = "/user/*")
public class UserAccessFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);

        // Check if session exists and has a "user" attribute
        if (session != null && session.getAttribute("user") != null) {
            // User is authenticated, proceed
            // Prevent Caching for secured pages
            res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
            res.setHeader("Pragma", "no-cache"); // HTTP 1.0.
            res.setDateHeader("Expires", 0); // Proxies.

            chain.doFilter(request, response);
        } else {
            // User is not authenticated, redirect to sign-in page
            // Since we are in /user/* context (e.g. /user/profile.html), sign-in is one
            // level up (../sign-in.html)
            // Or absolute path /sign-in.html based on context root
            res.sendRedirect(req.getContextPath() + "/index.html");
        }
    }

    @Override
    public void destroy() {
        // Cleanup code if needed
    }
}

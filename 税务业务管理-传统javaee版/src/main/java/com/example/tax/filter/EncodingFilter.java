package com.example.tax.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * First filter in the chain: UTF-8 for both directions, a per-request trace id,
 * and basic browser hardening headers.
 */
public class EncodingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // no configuration
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            httpRequest.setAttribute("traceId", traceId);
            httpResponse.setHeader("X-Trace-Id", traceId);
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            httpResponse.setHeader("Referrer-Policy", "same-origin");
            httpResponse.setHeader("Content-Security-Policy",
                    "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                            + "script-src 'self'; font-src 'self'; object-src 'none'; "
                            + "base-uri 'self'; form-action 'self'; frame-ancestors 'self'");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to release
    }
}

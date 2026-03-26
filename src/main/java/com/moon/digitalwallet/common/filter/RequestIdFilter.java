package com.moon.digitalwallet.common.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
	public static final String REQUEST_ID_HEADER = "X-Request-Id";
	public static final String REQUEST_ID_ATTRIBUTE = "requestId";

	private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		long startNanos = System.nanoTime();
		String requestId = request.getHeader(REQUEST_ID_HEADER);

		if (requestId == null || requestId.isEmpty()) {
			requestId = UUID.randomUUID().toString();
		}

		request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);
		MDC.put("requestId", requestId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
			MDC.put("httpMethod", request.getMethod());
			MDC.put("httpPath", request.getRequestURI());
			MDC.put("httpStatus", String.valueOf(response.getStatus()));
			MDC.put("durationMs", String.valueOf(durationMs));
			log.info(
				"event=http_request_completed method={} path={} status={} durationMs={}",
				request.getMethod(),
				request.getRequestURI(),
				response.getStatus(),
				durationMs
			);
			MDC.remove("durationMs");
			MDC.remove("httpStatus");
			MDC.remove("httpPath");
			MDC.remove("httpMethod");
			MDC.remove("requestId");
		}
	}
}

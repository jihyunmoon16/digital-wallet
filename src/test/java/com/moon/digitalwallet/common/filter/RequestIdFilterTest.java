package com.moon.digitalwallet.common.filter;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class RequestIdFilterTest {
	private RequestIdFilter filter = new RequestIdFilter();

	@Test
	void doFilter_withRequestIdHeader_registerRequestIdInMDC() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Request-Id", "req-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		AtomicReference<String> capturedRequestId = new AtomicReference<>();
		FilterChain filterChain = (req, res) -> capturedRequestId.set(MDC.get("requestId"));

		// when
		filter.doFilter(request, response, filterChain);

		// then
		assertThat(capturedRequestId.get()).isEqualTo("req-123");
	}

	@Test
	void doFilter_withoutRequestIdHeader_registerGeneratedRequestIdInMDC() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		AtomicReference<String> capturedRequestId = new AtomicReference<>();
		FilterChain filterChain = (req, res) -> {
			capturedRequestId.set(MDC.get("requestId"));
		};

		// when
		filter.doFilter(request, response, filterChain);

		// then
		assertThat(capturedRequestId.get()).isNotNull();
		assertThat(capturedRequestId.get()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	}

	@Test
	void doFilter_clearsRequestIdFromMDCAfterProcessing() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Request-Id", "req-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (req, res) -> {
			assertThat(MDC.get("requestId")).isEqualTo("req-123");
		};

		// when
		filter.doFilter(request, response, filterChain);

		// then
		assertThat(MDC.get("requestId")).isNull();
	}

}

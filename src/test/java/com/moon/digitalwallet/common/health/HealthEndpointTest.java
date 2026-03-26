package com.moon.digitalwallet.common.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthEndpointTest {

	@LocalServerPort
	private int port;

	@Test
	void health_returnsUp() throws Exception {
		HttpResponse<String> response = get("/actuator/health");
		String body = compact(response.body());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(body).contains("\"status\":\"UP\"");
	}

	@Test
	void liveness_returnsUp() throws Exception {
		HttpResponse<String> response = get("/actuator/health/liveness");
		String body = compact(response.body());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(body).contains("\"status\":\"UP\"");
		assertThat(body).contains("\"ping\"");
	}

	@Test
	void readiness_includesDbAndRedis() throws Exception {
		HttpResponse<String> response = get("/actuator/health/readiness");
		String body = compact(response.body());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(body).contains("\"status\":\"UP\"");
		assertThat(body).contains("\"db\"");
		assertThat(body).contains("\"redis\"");
		assertThat(body).contains("\"ping\"");
	}

	private String baseUrl(String path) {
		return "http://localhost:" + port + path;
	}

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl(path)))
			.GET()
			.build();

		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	private String compact(String body) {
		return body.replace(" ", "").replace("\n", "");
	}
}

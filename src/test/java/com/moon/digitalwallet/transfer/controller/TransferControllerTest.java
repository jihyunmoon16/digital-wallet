package com.moon.digitalwallet.transfer.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.idempotency.service.IdempotentTransferService;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IdempotentTransferService idempotentTransferService;

	@Test
	void transfer_success() throws Exception {
		when(idempotentTransferService.transfer(anyString(), anyLong(), anyLong(), any(BigDecimal.class)))
			.thenReturn(1001L);

		String request = """
	        {
          "fromAccountId": 1,
          "toAccountId": 2,
          "amount": 3000
        }
        """;

		mockMvc.perform(post("/transfers")
					.header("Idempotency-Key", "idem-key-1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transferId").value(1001L));
	}

	@Test
	void transfer_insufficientBalance_returns422() throws Exception {

		when(idempotentTransferService.transfer(anyString(), anyLong(), anyLong(), any(BigDecimal.class)))
			.thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

		String request = """
		{
		  "fromAccountId": 1,
		  "toAccountId": 2,
		  "amount": 3000
		}
		""";

		mockMvc.perform(post("/transfers")
					.header("X-Request-Id", "req-422")
					.header("Idempotency-Key", "idem-key-422")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
			.andExpect(jsonPath("$.message").value("insufficient balance"))
			.andExpect(jsonPath("$.requestId").value("req-422"));
	}

	@Test
	void transfer_concurrentModification_returns409WithErrorCode() throws Exception {

		when(idempotentTransferService.transfer(anyString(), anyLong(), anyLong(), any(BigDecimal.class)))
			.thenThrow(new BusinessException(ErrorCode.CONCURRENT_MODIFICATION));

		String request = """
		{
		  "fromAccountId": 1,
		  "toAccountId": 2,
		  "amount": 3000
		}
		""";

		mockMvc.perform(post("/transfers")
					.header("X-Request-Id", "req-123")
					.header("Idempotency-Key", "idem-key-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
			.andExpect(jsonPath("$.message").value("concurrent modification"))
				.andExpect(jsonPath("$.requestId").value("req-123"));
	}

	@Test
	void transfer_withoutIdempotencyKey_returns400() throws Exception {
		String request = """
		{
		  "fromAccountId": 1,
		  "toAccountId": 2,
		  "amount": 3000
		}
		""";

		mockMvc.perform(post("/transfers")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isBadRequest());

		verify(idempotentTransferService, never()).transfer(anyString(), anyLong(), anyLong(), any(BigDecimal.class));
	}

}

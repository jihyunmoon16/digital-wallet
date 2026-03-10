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
import com.moon.digitalwallet.transfer.service.TransferService;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TransferService transferService;

	@Test
	void transfer_success() throws Exception {

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
			.andExpect(status().isOk());
	}

	@Test
	void transfer_insufficientBalance_returns422() throws Exception {

		doThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE)).when(transferService)
			.transfer(anyLong(), anyLong(), any(BigDecimal.class));

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
			.andExpect(status().isUnprocessableContent());
	}

}

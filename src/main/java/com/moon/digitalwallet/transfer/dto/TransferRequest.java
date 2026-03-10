package com.moon.digitalwallet.transfer.dto;

import java.math.BigDecimal;

public record TransferRequest(
	Long fromAccountId,
	Long toAccountId,
	BigDecimal amount) {
}

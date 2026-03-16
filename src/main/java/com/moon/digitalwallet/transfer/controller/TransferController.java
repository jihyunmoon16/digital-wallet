package com.moon.digitalwallet.transfer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moon.digitalwallet.transfer.dto.TransferRequest;
import com.moon.digitalwallet.transfer.dto.TransferResponse;
import com.moon.digitalwallet.idempotency.service.IdempotentTransferService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {
	private final IdempotentTransferService idempotentTransferService;

	@PostMapping
	public ResponseEntity<TransferResponse> transfer(
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@RequestBody TransferRequest request) {
		Long transferId = idempotentTransferService.transfer(
			idempotencyKey,
			request.fromAccountId(),
			request.toAccountId(),
			request.amount()
		);
		return ResponseEntity.ok(new TransferResponse(transferId));
	}
}

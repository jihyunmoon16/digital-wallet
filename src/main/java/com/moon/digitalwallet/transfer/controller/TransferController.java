package com.moon.digitalwallet.transfer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moon.digitalwallet.transfer.dto.TransferRequest;
import com.moon.digitalwallet.transfer.service.TransferService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {
	private final TransferService transferService;

	@PostMapping
	public ResponseEntity<Void> transfer(@RequestBody TransferRequest request) {
		transferService.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
		return ResponseEntity.ok().build();
	}
}

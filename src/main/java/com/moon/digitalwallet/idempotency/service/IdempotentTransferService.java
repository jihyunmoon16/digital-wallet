package com.moon.digitalwallet.idempotency.service;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.service.TransferService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotentTransferService {

	private static final String KEY_PREFIX = "idem:transfer:";
	private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(30);
	private static final Duration SUCCESS_TTL = Duration.ofHours(24);
	private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
	private static final String STATUS_SUCCESS = "SUCCESS";

	private final StringRedisTemplate redisTemplate;
	private final TransferService transferService;

	public Long transfer(String idempotencyKey, Long accountFromId, Long accountToId, BigDecimal amount) {
		String key = KEY_PREFIX + idempotencyKey;
		String requestHash = requestHash(accountFromId, accountToId, amount);

		Boolean acquired = redisTemplate.opsForValue()
			.setIfAbsent(key, encodeInProgress(requestHash), IN_PROGRESS_TTL);

		if (Boolean.TRUE.equals(acquired)) {
			try {
				Long transferId = transferService.transfer(accountFromId, accountToId, amount);
				redisTemplate.opsForValue().set(key, encodeSuccess(requestHash, transferId), SUCCESS_TTL);
				return transferId;
			} catch (RuntimeException e) {
				redisTemplate.delete(key);
				throw e;
			}
		}

		String cachedValue = redisTemplate.opsForValue().get(key);
		if (cachedValue == null) {
			throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS);
		}

		CachedTransfer cachedTransfer = CachedTransfer.parse(cachedValue);
		if (!requestHash.equals(cachedTransfer.requestHash())) {
			throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
		}

		if (STATUS_SUCCESS.equals(cachedTransfer.status())) {
			return cachedTransfer.transferId();
		}

		throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS);
	}

	private String requestHash(Long accountFromId, Long accountToId, BigDecimal amount) {
		return accountFromId + ":" + accountToId + ":" + amount.stripTrailingZeros().toPlainString();
	}

	private String encodeInProgress(String requestHash) {
		return STATUS_IN_PROGRESS + "|" + requestHash;
	}

	private String encodeSuccess(String requestHash, Long transferId) {
		return STATUS_SUCCESS + "|" + requestHash + "|" + transferId;
	}

	private record CachedTransfer(String status, String requestHash, Long transferId) {
		private static CachedTransfer parse(String value) {
			String[] tokens = value.split("\\|", 3);
			if (tokens.length < 2) {
				throw new IllegalStateException("invalid idempotency cache format");
			}

			String status = tokens[0];
			String requestHash = tokens[1];
			Long transferId = null;
			if (STATUS_SUCCESS.equals(status) && tokens.length == 3) {
				transferId = Long.parseLong(tokens[2]);
			}
			return new CachedTransfer(status, requestHash, transferId);
		}
	}
}

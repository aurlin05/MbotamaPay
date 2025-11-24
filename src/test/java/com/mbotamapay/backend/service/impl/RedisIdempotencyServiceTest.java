package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.idempotency.IdempotencyRecord;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.entity.IdempotencyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisIdempotencyService idempotencyService;

    private static final String TEST_KEY = "test-key-123";
    private static final String REDIS_KEY = "idempotency:" + TEST_KEY;

    @BeforeEach
    void setUp() {
        // Setup is done per test to avoid unnecessary stubbing warnings
    }

    @Test
    void checkIdempotency_shouldReturnEmpty_whenKeyDoesNotExist() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        // When
        Optional<TransactionResponse> result = idempotencyService.checkIdempotency(TEST_KEY);

        // Then
        assertThat(result).isEmpty();
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    void checkIdempotency_shouldReturnEmpty_whenKeyIsNull() {
        // When
        Optional<TransactionResponse> result = idempotencyService.checkIdempotency(null);

        // Then
        assertThat(result).isEmpty();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void checkIdempotency_shouldReturnEmpty_whenKeyIsBlank() {
        // When
        Optional<TransactionResponse> result = idempotencyService.checkIdempotency("   ");

        // Then
        assertThat(result).isEmpty();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void checkIdempotency_shouldReturnResult_whenCompletedRecordExists() {
        // Given
        TransactionResponse expectedResponse = TransactionResponse.builder()
                .id(1L)
                .reference("TXN-123")
                .amount(BigDecimal.valueOf(100))
                .status("COMPLETED")
                .build();

        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(TEST_KEY)
                .result(expectedResponse)
                .status(IdempotencyStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(record);

        // When
        Optional<TransactionResponse> result = idempotencyService.checkIdempotency(TEST_KEY);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResponse);
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    void checkIdempotency_shouldReturnEmpty_whenRecordIsProcessing() {
        // Given
        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(TEST_KEY)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(record);

        // When
        Optional<TransactionResponse> result = idempotencyService.checkIdempotency(TEST_KEY);

        // Then
        assertThat(result).isEmpty();
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    void storeIdempotencyResult_shouldStoreRecordWithTransaction() {
        // Given
        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .reference("TXN-123")
                .amount(BigDecimal.valueOf(100))
                .status("COMPLETED")
                .build();

        when(redisTemplate.execute(any(SessionCallback.class))).thenReturn(null);

        // When
        idempotencyService.storeIdempotencyResult(TEST_KEY, response);

        // Then
        verify(redisTemplate).execute(any(SessionCallback.class));
    }

    @Test
    void storeIdempotencyResult_shouldNotStore_whenKeyIsNull() {
        // Given
        TransactionResponse response = TransactionResponse.builder().build();

        // When
        idempotencyService.storeIdempotencyResult(null, response);

        // Then
        verify(redisTemplate, never()).execute(any(SessionCallback.class));
    }

    @Test
    void isProcessing_shouldReturnTrue_whenRecordIsProcessing() {
        // Given
        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(TEST_KEY)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(record);

        // When
        boolean result = idempotencyService.isProcessing(TEST_KEY);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    void isProcessing_shouldReturnFalse_whenRecordIsCompleted() {
        // Given
        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(TEST_KEY)
                .status(IdempotencyStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(record);

        // When
        boolean result = idempotencyService.isProcessing(TEST_KEY);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    void isProcessing_shouldReturnFalse_whenKeyDoesNotExist() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        // When
        boolean result = idempotencyService.isProcessing(TEST_KEY);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    void markAsProcessing_shouldSetRecordWithTTL() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(REDIS_KEY), any(IdempotencyRecord.class), eq(Duration.ofHours(24))))
                .thenReturn(true);

        // When
        idempotencyService.markAsProcessing(TEST_KEY);

        // Then
        ArgumentCaptor<IdempotencyRecord> recordCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(valueOperations).setIfAbsent(eq(REDIS_KEY), recordCaptor.capture(), eq(Duration.ofHours(24)));

        IdempotencyRecord capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.getKey()).isEqualTo(TEST_KEY);
        assertThat(capturedRecord.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        assertThat(capturedRecord.getCreatedAt()).isNotNull();
        assertThat(capturedRecord.getExpiresAt()).isNotNull();
    }

    @Test
    void markAsProcessing_shouldNotSet_whenKeyAlreadyExists() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(REDIS_KEY), any(IdempotencyRecord.class), eq(Duration.ofHours(24))))
                .thenReturn(false);

        // When
        idempotencyService.markAsProcessing(TEST_KEY);

        // Then
        verify(valueOperations).setIfAbsent(eq(REDIS_KEY), any(IdempotencyRecord.class), eq(Duration.ofHours(24)));
    }

    @Test
    void markAsProcessing_shouldNotSet_whenKeyIsNull() {
        // When
        idempotencyService.markAsProcessing(null);

        // Then
        verify(valueOperations, never()).setIfAbsent(anyString(), any(), any(Duration.class));
    }
}

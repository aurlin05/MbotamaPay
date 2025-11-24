package com.mbotamapay.backend.dto.idempotency;

import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.entity.IdempotencyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyRecord implements Serializable {
    private String key;
    private TransactionResponse result;
    private IdempotencyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

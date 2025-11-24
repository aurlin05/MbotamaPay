package com.mbotamapay.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatsResponse {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalTransactions;
    private BigDecimal totalFundsCirculating;
}

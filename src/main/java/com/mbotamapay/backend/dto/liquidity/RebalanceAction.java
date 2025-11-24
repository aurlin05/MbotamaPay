package com.mbotamapay.backend.dto.liquidity;

import com.mbotamapay.backend.entity.OperatorAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceAction {
    private OperatorAccount.Provider fromProvider;
    private OperatorAccount.Provider toProvider;
    private BigDecimal amount;
    private String currency;
    private String reason;
}

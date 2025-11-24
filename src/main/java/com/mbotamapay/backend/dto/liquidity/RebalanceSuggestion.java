package com.mbotamapay.backend.dto.liquidity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceSuggestion {
    private List<RebalanceAction> actions;
    private String analysisTimestamp;
}

package com.mbotamapay.backend.dto.bridge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BridgeTransferResponse {
    private String referenceId;
    private BridgeTransferStatus status;
    private String fromProviderTxId;
    private String toProviderTxId;
    private String message;
}

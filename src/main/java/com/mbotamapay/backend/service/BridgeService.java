package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.bridge.BridgeTransferRequest;
import com.mbotamapay.backend.dto.bridge.BridgeTransferResponse;
import com.mbotamapay.backend.dto.bridge.BridgeTransferStatus;

public interface BridgeService {

    /**
     * Initiate a bridge transfer between providers.
     * Usually triggered by a webhook (deposit confirmed) or API call.
     * 
     * @param request Transfer details
     * @return Response with reference ID and status
     */
    BridgeTransferResponse bridgeTransfer(BridgeTransferRequest request);

    /**
     * Check status of a bridge transfer
     * 
     * @param referenceId Bridge reference ID
     * @return Current status
     */
    BridgeTransferStatus checkBridgeStatus(String referenceId);
}

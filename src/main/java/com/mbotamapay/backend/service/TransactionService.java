package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.transaction.TransferRequest;
import com.mbotamapay.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {
    TransactionResponse sendMoney(User sender, TransferRequest request);

    TransactionResponse sendMoneyByEmail(User sender, String recipientEmail, BigDecimal amount, String description);

    List<TransactionResponse> getTransactionHistory(User user);

    Page<TransactionResponse> getTransactionHistoryPaginated(User user, Pageable pageable);
}

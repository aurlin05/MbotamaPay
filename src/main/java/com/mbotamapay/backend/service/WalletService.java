package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.wallet.WalletResponse;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.entity.Wallet;

import java.math.BigDecimal;

public interface WalletService {
    Wallet createWallet(User user);

    WalletResponse getBalance(Long userId);

    void credit(Long walletId, BigDecimal amount);

    void debit(Long walletId, BigDecimal amount);

    Wallet getWalletByUser(User user);
}

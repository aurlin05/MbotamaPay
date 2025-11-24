package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.admin.AdminStatsResponse;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.user.UserResponse;

import java.util.List;

public interface AdminService {
    List<UserResponse> getAllUsers();

    List<TransactionResponse> getAllTransactions();

    AdminStatsResponse getStats();

    void banUser(Long userId);

    void unbanUser(Long userId);
}

package com.mbotamapay.backend.mapper;

import com.mbotamapay.backend.dto.wallet.WalletResponse;
import com.mbotamapay.backend.entity.Wallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletMapper {
    WalletResponse toResponse(Wallet wallet);
}

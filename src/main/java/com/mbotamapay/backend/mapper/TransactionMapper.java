package com.mbotamapay.backend.mapper;

import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "type", expression = "java(transaction.getType().name())")
    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    @Mapping(target = "senderEmail", source = "senderWallet.user.email")
    @Mapping(target = "receiverEmail", source = "receiverWallet.user.email")
    TransactionResponse toResponse(Transaction transaction);
}

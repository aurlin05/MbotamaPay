package com.mbotamapay.backend.mapper;

import com.mbotamapay.backend.dto.user.UserResponse;
import com.mbotamapay.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "kycLevel", expression = "java(user.getKycLevel().name())")
    UserResponse toResponse(User user);
}

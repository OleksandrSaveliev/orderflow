package com.my.orderflow.mapper;

import com.my.orderflow.dto.auth.RegisterRequestDto;
import com.my.orderflow.model.User;
import com.my.orderflow.model.enums.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequestDto dto);

    default User toEntityWithRole(RegisterRequestDto dto, Role role) {
        User user = toEntity(dto);
        user.setRole(role);
        return user;
    }
}
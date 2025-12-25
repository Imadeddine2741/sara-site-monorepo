package com.sararahmani.site.backend.service.mapper;

import com.sararahmani.site.backend.dto.AuthResponse;
import com.sararahmani.site.backend.dto.RegisterRequest;
import com.sararahmani.site.backend.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", imports = {com.sararahmani.site.backend.entity.Role.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)   // hashé après
    @Mapping(target = "role", expression = "java(Role.PATIENT)")
    @Mapping(target = "enabled", expression = "java(false)")
    User fromRegister(RegisterRequest request);

    @Mapping(target = "token", source = "token")
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    AuthResponse toAuthResponse(User user, String token);
}


package com.yiliao.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        String realName,
        Integer userType,
        List<String> roles
) {
}

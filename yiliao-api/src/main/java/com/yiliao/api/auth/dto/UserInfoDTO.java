package com.yiliao.api.auth.dto;

import java.util.List;

/**
 * 用户简要信息（跨服务显示操作者姓名等场景）。
 */
public record UserInfoDTO(
        Long userId,
        String realName,
        Integer userType,
        List<String> roles
) {
}

package com.yiliao.api.auth;

import com.yiliao.api.auth.dto.UserInfoDTO;
import com.yiliao.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * auth 服务对外契约（specs/global/30 §5）。
 * 方法路径为绝对路径（服务端实现类直接映射）；internal 路径仅供服务间调用，网关拒绝外部访问。
 */
@FeignClient(name = "yiliao-auth", contextId = "authApi")
public interface AuthApi {

    @GetMapping("/api/auth/internal/users/{id}")
    Result<UserInfoDTO> getUserInfo(@PathVariable("id") Long id);

    @PostMapping("/api/auth/internal/users/batch")
    Result<List<UserInfoDTO>> batchUserInfos(@RequestBody List<Long> ids);
}

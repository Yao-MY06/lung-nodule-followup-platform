package com.yiliao.auth.feign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.api.auth.AuthApi;
import com.yiliao.api.auth.dto.UserInfoDTO;
import com.yiliao.auth.entity.SysUser;
import com.yiliao.auth.mapper.SysRoleMapper;
import com.yiliao.auth.mapper.SysUserMapper;
import com.yiliao.common.core.result.Result;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AuthApi 契约实现（specs/global/30 §5）：@RestController 实现契约接口，编译期强约束同步。
 * 路径 /api/auth/internal/** 仅供服务间调用，网关拒绝外部访问。
 */
@RestController
public class AuthApiController implements AuthApi {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public AuthApiController(SysUserMapper userMapper, SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public Result<UserInfoDTO> getUserInfo(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.ok(null);
        }
        return Result.ok(toDTO(user));
    }

    @Override
    public Result<List<UserInfoDTO>> batchUserInfos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.ok(List.of());
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));
        return Result.ok(users.stream().map(this::toDTO).toList());
    }

    private UserInfoDTO toDTO(SysUser user) {
        return new UserInfoDTO(user.getId(), user.getRealName(), user.getUserType(),
                roleMapper.selectRoleCodes(user.getId()));
    }
}

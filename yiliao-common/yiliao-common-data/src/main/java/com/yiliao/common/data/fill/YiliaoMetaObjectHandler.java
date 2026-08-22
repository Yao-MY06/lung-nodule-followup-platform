package com.yiliao.common.data.fill;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 字段自动填充（specs/global/20 §1）：createTime/updateTime/createBy/updateBy。
 */
public class YiliaoMetaObjectHandler implements MetaObjectHandler {

    private final UserContextProvider userContextProvider;

    public YiliaoMetaObjectHandler(UserContextProvider userContextProvider) {
        this.userContextProvider = userContextProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserIdOrNull());
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserIdOrNull());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserIdOrNull());
    }

    private Long currentUserIdOrNull() {
        return userContextProvider != null ? userContextProvider.currentUserId() : null;
    }
}

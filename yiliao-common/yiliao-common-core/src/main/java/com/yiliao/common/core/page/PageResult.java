package com.yiliao.common.core.page;

import java.util.List;

/**
 * 分页返回（specs/global/10 §2）。与前端约定的统一分页结构。
 */
public record PageResult<T>(List<T> records, long total, long page, long size) {

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        return new PageResult<>(records, total, page, size);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(List.of(), 0, page, size);
    }
}

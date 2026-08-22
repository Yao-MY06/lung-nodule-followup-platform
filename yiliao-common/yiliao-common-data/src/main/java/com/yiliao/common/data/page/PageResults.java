package com.yiliao.common.data.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiliao.common.core.page.PageResult;

import java.util.function.Function;

/**
 * MyBatis-Plus 分页 → 统一 PageResult 转换（specs/global/10 §2）。
 */
public final class PageResults {

    private PageResults() {
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> converter) {
        return PageResult.of(
                page.getRecords().stream().map(converter).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }
}

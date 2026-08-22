package com.yiliao.common.core.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页请求（specs/global/20 §5）。sortBy 必须在服务端白名单校验，防 SQL 注入。
 */
public class PageQuery {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer size = 10;

    private String sortBy;

    /** asc / desc，默认 desc */
    private String sortOrder = "desc";

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}

package com.homecare.serviceorder.common;

import lombok.Data;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {

    private long total;
    private int page;
    private int pageSize;
    private List<T> records;

    public PageResult(long total, int page, int pageSize, List<T> records) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.records = records;
    }
}

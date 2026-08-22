package com.yiliao.common.core.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void defaultsArePageOneSizeTen() {
        PageQuery q = new PageQuery();
        assertEquals(1, q.getPage());
        assertEquals(10, q.getSize());
        assertEquals(0, q.offset());
    }

    @Test
    void offsetComputesFromPage() {
        PageQuery q = new PageQuery();
        q.setPage(3);
        q.setSize(20);
        assertEquals(40, q.offset());
    }

    @Test
    void emptyFactory() {
        PageResult<String> empty = PageResult.empty(2, 10);
        assertEquals(0, empty.total());
        assertEquals(0, empty.records().size());
        assertEquals(2, empty.page());
    }

    @Test
    void ofFactoryKeepsValues() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 42, 3, 2);
        assertEquals(42, page.total());
        assertEquals(2, page.records().size());
    }
}

package com.yiliao.ai.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataFilterTest {

    @Test
    void phoneMasked() {
        String out = SensitiveDataFilter.sanitize("患者张三联系电话13812345678复查");
        assertFalse(out.contains("13812345678"));
        assertTrue(out.contains("[电话]"));
        assertFalse(SensitiveDataFilter.containsSensitive(out));
    }

    @Test
    void idCardMasked() {
        String out = SensitiveDataFilter.sanitize("证件号110101196503120011，右肺结节");
        assertFalse(out.contains("110101196503120011"));
        assertTrue(out.contains("[证件号]"));
    }

    @Test
    void emailMasked() {
        String out = SensitiveDataFilter.sanitize("联系 doctor@example.com 复诊");
        assertFalse(out.contains("doctor@example.com"));
    }

    @Test
    void medicalRecordNumberMasked() {
        String out = SensitiveDataFilter.sanitize("住院号：20260822001，报告如下");
        assertFalse(out.contains("20260822001"));
        assertTrue(out.contains("[已隐藏]"));
    }

    @Test
    void normalTextUntouched() {
        String text = "右肺上叶见磨玻璃结节，最大径约 5mm，边界清。";
        assertTrue(SensitiveDataFilter.sanitize(text).contains("5mm"));
    }
}

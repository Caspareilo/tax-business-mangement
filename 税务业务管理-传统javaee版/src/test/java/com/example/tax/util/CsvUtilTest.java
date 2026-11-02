package com.example.tax.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvUtilTest {

    @Test
    @DisplayName("SEARCH-05 CSV 单元格转义与公式注入防护")
    void csvEscaping() {
        assertEquals("\"'=SUM(A1:A2)\"", CsvUtil.escape("=SUM(A1:A2)"));
        assertEquals("\"'+1\"", CsvUtil.escape("+1"));
        assertEquals("\"'-1\"", CsvUtil.escape("-1"));
        assertEquals("\"'@cmd\"", CsvUtil.escape("@cmd"));
        assertEquals("\"a,b\"", CsvUtil.escape("a,b"));
        assertEquals("\"a\"\"b\"", CsvUtil.escape("a\"b"));
        assertEquals("普通文本", CsvUtil.escape("普通文本"));
        assertEquals("", CsvUtil.escape(null));
    }
}

package com.sosuisha.storymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutputFormatTest {
    @Test
    @DisplayName("形式の名前は、大文字と小文字を区別しない")
    void format_name_ignores_case() {
        assertEquals(OutputFormat.PNG, OutputFormat.fromName("PNG"));
    }

    @Test
    @DisplayName("未知の形式の名前は、受け付けない")
    void unknown_format_name_is_rejected() {
        assertThrows(IllegalArgumentException.class, () -> OutputFormat.fromName("pdf"));
    }
}

package com.zjgsu.whattoeat.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class XssSanitizerTest {

    @Test
    void sanitizeShouldRemoveScriptTags() {
        String input = "<script>alert('xss')</script>Hello";
        String result = XssSanitizer.sanitize(input);
        assertEquals("Hello", result);
    }

    @Test
    void sanitizeShouldPreserveSafeHtml() {
        String input = "<b>Bold</b> and <i>italic</i>";
        String result = XssSanitizer.sanitize(input);
        assertEquals("<b>Bold</b> and <i>italic</i>", result);
    }

    @Test
    void sanitizeShouldRemoveEventHandlers() {
        String input = "<img src=x onerror=alert(1)>";
        String result = XssSanitizer.sanitize(input);
        assertEquals("", result);
    }

    @Test
    void sanitizeShouldHandleNullInput() {
        assertNull(XssSanitizer.sanitize(null));
    }

    @Test
    void sanitizeShouldHandlePlainTextInput() {
        String input = "Normal text without HTML";
        String result = XssSanitizer.sanitize(input);
        assertEquals("Normal text without HTML", result);
    }

    @Test
    void sanitizeShouldRemoveJavascriptProtocol() {
        String input = "<a href=\"javascript:alert(1)\">click</a>";
        String result = XssSanitizer.sanitize(input);
        assertEquals("click", result);
    }

    @Test
    void stripAllShouldRemoveAllHtmlTags() {
        String input = "<b>Bold</b> <script>alert(1)</script>text";
        String result = XssSanitizer.stripAll(input);
        assertEquals("Bold text", result);
    }

    @Test
    void stripAllShouldHandleNullInput() {
        assertNull(XssSanitizer.stripAll(null));
    }
}

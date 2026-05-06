package com.zjgsu.whattoeat.common.security;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public final class XssSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES);

    private XssSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return POLICY.sanitize(input);
    }

    public static String stripAll(String input) {
        if (input == null) {
            return null;
        }
        return org.owasp.html.Sanitizers.BLOCKS.sanitize(input);
    }
}

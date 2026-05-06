package com.zjgsu.whattoeat.common.security;

import org.owasp.html.PolicyFactory;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.Sanitizers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XssSanitizer {

    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);");
    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES);
    private static final PolicyFactory PLAIN_TEXT_POLICY = new HtmlPolicyBuilder().toFactory();

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
        return decodeSafeNumericEntities(PLAIN_TEXT_POLICY.sanitize(input));
    }

    private static String decodeSafeNumericEntities(String value) {
        Matcher matcher = NUMERIC_ENTITY.matcher(value);
        StringBuilder decoded = new StringBuilder();
        while (matcher.find()) {
            int codePoint = parseEntityCodePoint(matcher.group(1));
            if (isSafeToDecode(codePoint)) {
                matcher.appendReplacement(decoded, Matcher.quoteReplacement(Character.toString(codePoint)));
            }
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    private static int parseEntityCodePoint(String value) {
        if (value.startsWith("x") || value.startsWith("X")) {
            return Integer.parseInt(value.substring(1), 16);
        }
        return Integer.parseInt(value);
    }

    private static boolean isSafeToDecode(int codePoint) {
        return codePoint != '<'
                && codePoint != '>'
                && codePoint != '&'
                && codePoint != '"'
                && codePoint != '\''
                && Character.isValidCodePoint(codePoint);
    }
}

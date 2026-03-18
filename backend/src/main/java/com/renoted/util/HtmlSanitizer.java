package com.renoted.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * HTML Sanitizer - XSS Prevention
 *
 * Whitelist approach: Only allow safe HTML tags
 * Remove ALL dangerous content
 */
public class HtmlSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            // Text formatting
            .allowElements("b", "i", "u", "s", "strong", "em", "code", "pre", "br")
            // Headings
            .allowElements("h1", "h2", "h3")
            // Paragraphs
            .allowElements("p")
            // Lists
            .allowElements("ul", "ol", "li")
            // Blockquote
            .allowElements("blockquote")
            // Links (safe protocols only)
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            .allowStandardUrlProtocols()
            .allowAttributes("target").matching(true, "_blank").onElements("a")
            .allowAttributes("rel").matching(true, "noopener", "noreferrer").onElements("a")
            .toFactory();

    public static String sanitize(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }

        String sanitized = POLICY.sanitize(html);

        if (!html.equals(sanitized)) {
            System.out.println("⚠️ XSS BLOCKED - Dangerous content removed!");
            System.out.println("Before: " + html.length() + " chars");
            System.out.println("After: " + sanitized.length() + " chars");
        }

        return sanitized;
    }

    public static boolean containsDangerousContent(String html) {
        if (html == null || html.trim().isEmpty()) {
            return false;
        }
        return !html.equals(POLICY.sanitize(html));
    }
}
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.roleplay;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.codeUtilities.MathUtility;

/**
 * Conversions between the journal's two text forms: plain text (Oracle log records) and the HTML that rich-text notes
 * are stored as.
 */
public final class JournalText {
    private static final String BULLET = "\uE000";
    private static final String HEADING = "\uE001";
    private static final String BOLD = "\uE002";
    private static final String ITALIC = "\uE003";
    private static final Pattern MARKDOWN_SPECIAL = Pattern.compile("[\\\\`*_#<>\\[\\]]");
    private static final Set<String> ALLOWED_TAGS = Set.of("html", "head", "body", "p", "b", "strong", "i", "em",
          "u", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "blockquote", "br");
    private static final Pattern HIDDEN_CONTENT = Pattern.compile(
          "(?is)<(script|style|title|object|iframe)\\b[^>]*>.*?</\\1\\s*>");
    private static final Pattern ANY_TAG = Pattern.compile("(?s)<(/?)([a-zA-Z][a-zA-Z0-9]*)\\b[^>]*>");
    private static final Pattern MARKDOWN_ESCAPE = Pattern.compile("\\\\([\\\\`*_#<>\\[\\]+.-])");
    private static final Pattern LIST_START = Pattern.compile("(?m)^(\\s*)([-+]|\\d+\\.)(?=\\s)");
    private static final Pattern HEAD = Pattern.compile("(?is)<head>.*?</head>");
    private static final Pattern BODY = Pattern.compile("(?is)<body[^>]*>(.*)</body>");
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(\\d+);");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");

    private JournalText() {}

    /**
     * @param text any text
     *
     * @return the text with HTML's special characters escaped
     */
    public static String escapeHtml(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * @param text plain text, possibly spanning several lines
     *
     * @return an HTML document showing the same text, with line breaks kept
     */
    public static String plainToHtml(final String text) {
        return "<html><body>" + escapeHtml(text).replace("\n", "<br>") + "</body></html>";
    }

    /**
     * @param text a stored note
     *
     * @return {@code true} if the note is already HTML rather than plain text from before notes were rich text
     */
    public static boolean isHtml(final String text) {
        return text.stripLeading().toLowerCase(Locale.ROOT).startsWith("<html");
    }

    /**
     * @param html an HTML document or fragment
     *
     * @return the markup inside {@code <body>}, or the whole input if it has no body, without the {@code <head>}
     */
    public static String bodyOf(final String html) {
        String withoutHead = HEAD.matcher(html).replaceAll("");
        Matcher body = BODY.matcher(withoutHead);
        String inner = body.find() ? body.group(1) : withoutHead;
        return inner.replaceAll("(?i)</?html>", "").strip();
    }

    /**
     * Flattens HTML to plain text: block ends and line breaks become new lines, list items become "- " bullets, and
     * every other tag is dropped.
     *
     * @param html an HTML document or fragment
     *
     * @return the plain text
     */
    public static String htmlToPlain(final String html) {
        return convert(html, false);
    }

    /**
     * Converts HTML to Markdown, keeping bold, italic, headings and bulleted lists. Underline has no Markdown form and
     * is dropped.
     *
     * @param html an HTML document or fragment
     *
     * @return the Markdown text
     */
    public static String htmlToMarkdown(final String html) {
        return convert(html, true);
    }

    private static String convert(final String html, final boolean markdown) {
        // Swing's HTML writer wraps long lines, so source line breaks mean nothing: only tags make new lines.
        String text = bodyOf(html).replaceAll("\\s*\\n\\s*", " ");
        // Markers stand in for the formatting until the player's own text has been escaped, so only their text is.
        text = text.replaceAll("(?i)<br\\s*/?>", "\n")
                     .replaceAll("(?i)</(p|div|ul|ol)>", "\n")
                     .replaceAll("(?i)<li[^>]*>", "\n" + BULLET + " ")
                     .replaceAll("(?i)</li>", "");
        if (markdown) {
            text = text.replaceAll("(?i)<h[1-6][^>]*>", "\n" + HEADING + " ")
                         .replaceAll("(?i)</h[1-6]>", "\n")
                         .replaceAll("(?i)<(b|strong)(\\s[^>]*)?>|</(b|strong)>", BOLD)
                         .replaceAll("(?i)<(i|em)(\\s[^>]*)?>|</(i|em)>", ITALIC);
        } else {
            text = text.replaceAll("(?i)</h[1-6]>", "\n");
        }
        text = TAG.matcher(text).replaceAll("");
        text = unescape(text);
        if (markdown) {
            text = escapeMarkdown(text);
        }
        text = text.lines().map(String::strip).reduce((first, second) -> first + "\n" + second).orElse("");
        // Swing indents block content, which leaves extra spaces after a bullet or heading marker.
        text = text.replaceAll("(?m)^" + BULLET + " +", "- ").replaceAll("(?m)^" + HEADING + " +", "### ")
                     .replace(BULLET, "-").replace(HEADING, "###").replace(BOLD, markdown ? "**" : "")
                     .replace(ITALIC, markdown ? "*" : "");
        return BLANK_LINES.matcher(text).replaceAll("\n\n").strip();
    }

    /**
     * Escapes the characters Markdown would read as formatting, so text such as "&lt;ECM&gt;", "Kell_Hounds_Two" or a
     * line starting "# " reads as written.
     *
     * @param text plain text
     *
     * @return the text, safe to place in a Markdown document
     */
    public static String escapeMarkdown(final String text) {
        String escaped = MARKDOWN_SPECIAL.matcher(text).replaceAll("\\\\$0");
        // A line starting with a list marker would become a list.
        return LIST_START.matcher(escaped).replaceAll("$1\\\\$2");
    }

    /**
     * Keeps only the tags the journal's editor writes (paragraphs, bold, italic, underline, headings, lists, quotes and
     * line breaks), without their attributes. Anything else, such as an image or link from an imported file, would
     * otherwise be loaded by the editor, fetching remote addresses.
     *
     * @param html an HTML document or fragment
     *
     * @return the HTML with every other tag removed, keeping its text
     */
    public static String sanitizeHtml(final String html) {
        final String withoutHidden = HIDDEN_CONTENT.matcher(html).replaceAll("");
        final Matcher tags = ANY_TAG.matcher(withoutHidden);
        final StringBuilder clean = new StringBuilder();
        while (tags.find()) {
            final String name = tags.group(2).toLowerCase(Locale.ROOT);
            final String replacement = ALLOWED_TAGS.contains(name) ? "<" + tags.group(1) + name + ">" : "";
            tags.appendReplacement(clean, Matcher.quoteReplacement(replacement));
        }
        tags.appendTail(clean);
        return clean.toString();
    }

    /**
     * Reverses {@link #escapeMarkdown(String)}.
     *
     * @param text Markdown text
     *
     * @return the text with its escaping backslashes removed
     */
    public static String unescapeMarkdown(final String text) {
        return MARKDOWN_ESCAPE.matcher(text).replaceAll("$1");
    }

    /** @return the character a numeric entity stands for, or the entity unchanged if it is not a valid one */
    private static String codePoint(final String digits, final String entity) {
        // An unreadable number comes back as -1, which is not a code point either.
        int value = MathUtility.parseInt(digits, -1);
        return Character.isValidCodePoint(value) ? new String(Character.toChars(value)) : entity;
    }

    private static String unescape(final String text) {
        Matcher numeric = NUMERIC_ENTITY.matcher(text);
        StringBuilder result = new StringBuilder();
        while (numeric.find()) {
            numeric.appendReplacement(result, Matcher.quoteReplacement(codePoint(numeric.group(1), numeric.group())));
        }
        numeric.appendTail(result);
        return result.toString()
                     .replace("&nbsp;", " ")
                     .replace("&lt;", "<")
                     .replace("&gt;", ">")
                     .replace("&quot;", "\"")
                     .replace("&#39;", "'")
                     .replace("&amp;", "&");
    }
}

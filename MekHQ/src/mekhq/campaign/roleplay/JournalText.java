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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversions between the journal's two text forms: plain text (Oracle log records) and the HTML that rich-text notes
 * are stored as.
 */
public final class JournalText {
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
        text = text.replaceAll("(?i)<br\\s*/?>", "\n")
                     .replaceAll("(?i)</(p|div|ul|ol)>", "\n")
                     .replaceAll("(?i)<li[^>]*>", "\n- ")
                     .replaceAll("(?i)</li>", "");
        if (markdown) {
            text = text.replaceAll("(?i)<h[1-6][^>]*>", "\n### ")
                         .replaceAll("(?i)</h[1-6]>", "\n")
                         .replaceAll("(?i)<(b|strong)(\\s[^>]*)?>|</(b|strong)>", "**")
                         .replaceAll("(?i)<(i|em)(\\s[^>]*)?>|</(i|em)>", "*");
        } else {
            text = text.replaceAll("(?i)</h[1-6]>", "\n");
        }
        text = TAG.matcher(text).replaceAll("");
        text = unescape(text);
        text = text.lines().map(String::strip).reduce((first, second) -> first + "\n" + second).orElse("");
        // Swing indents block content, which leaves extra spaces after a bullet or heading marker.
        text = text.replaceAll("(?m)^- +", "- ").replaceAll("(?m)^### +", "### ");
        return BLANK_LINES.matcher(text).replaceAll("\n\n").strip();
    }

    private static String unescape(final String text) {
        Matcher numeric = NUMERIC_ENTITY.matcher(text);
        StringBuilder result = new StringBuilder();
        while (numeric.find()) {
            numeric.appendReplacement(result,
                  Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(numeric.group(1)))));
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

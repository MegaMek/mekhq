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
package mekhq.campaign.utilities.glossary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the HTML a glossary definition is written in: its plain text, for searching, and the other entries it links
 * to.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class GlossaryText {
    /** The prefix of a link to another glossary entry, as in {@code <a href='GLOSSARY:KEY'>}. */
    public static final String GLOSSARY_LINK_PREFIX = "GLOSSARY:";
    /** The prefix of a link to a documentation entry, as in {@code <a href='DOCUMENTATION:KEY'>}. */
    public static final String DOCUMENTATION_LINK_PREFIX = "DOCUMENTATION:";

    // Only the tags glossary text uses are stripped, so a bare address written as <https://...> keeps its text.
    private static final String TAG_NAMES = "p|b|i|u|a|br|li|ul|ol|h[1-6]|td|tr|th|table|tbody|thead|blockquote|div"
                                                  + "|span|em|strong|font|center|hr|sup|sub|code|pre|img|html|body";
    private static final Pattern BLOCK_TAG = Pattern.compile(
          "</?(p|br|li|ul|ol|h[1-6]|td|tr|th|table|blockquote|div|hr)\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_TAG = Pattern.compile("</?(" + TAG_NAMES + ")\\b[^>]*>",
          Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK = Pattern.compile("href\\s*=\\s*['\"]([A-Z]+):([^'\"]+)['\"]",
          Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private GlossaryText() {
    }

    /**
     * Turns glossary HTML into plain text: tags are dropped, the common entities are decoded, and the words either
     * side of a paragraph or line break are kept apart.
     *
     * @param html the HTML
     *
     * @return the text, with runs of white space collapsed to single spaces
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String plainText(String html) {
        String text = BLOCK_TAG.matcher(html).replaceAll(" ");
        text = ANY_TAG.matcher(text).replaceAll("");
        text = decodeEntities(text);
        return SPACES.matcher(text).replaceAll(" ").strip();
    }

    /**
     * Lists the glossary entries a definition links to, in the order they are first linked.
     *
     * @param html the definition
     *
     * @return the lookup names of the linked entries, without repeats
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> glossaryLinks(String html) {
        return links(html, GLOSSARY_LINK_PREFIX);
    }

    /**
     * Lists the documentation entries a definition links to, in the order they are first linked.
     *
     * @param html the definition
     *
     * @return the lookup names of the linked documents, without repeats
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> documentationLinks(String html) {
        return links(html, DOCUMENTATION_LINK_PREFIX);
    }

    private static List<String> links(String html, String prefix) {
        String command = prefix.substring(0, prefix.length() - 1);
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = LINK.matcher(html);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(command)) {
                found.add(matcher.group(2).strip());
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * Decodes the character entities glossary text uses. {@code &amp;} is decoded last, so {@code &amp;lt;} reads as
     * the text {@code &lt;}.
     */
    static String decodeEntities(String text) {
        return text.replace("&nbsp;", " ")
                     .replace("&lt;", "<")
                     .replace("&gt;", ">")
                     .replace("&quot;", "\"")
                     .replace("&#39;", "'")
                     .replace("&apos;", "'")
                     .replace("&amp;", "&");
    }
}

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
package mekhq.gui.dialog.markets.contractMarket;

import java.util.Locale;
import java.util.regex.Pattern;

import megamek.client.ui.util.DiscordFormat;
import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Row;
import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Section;

/**
 * Formats a single offer as a Discord message, in the same spirit as MegaMekLab's "Export for Discord".
 *
 * <p>The message opens with the offer's name and employer and the briefing as a quote, in ordinary Discord Markdown,
 * followed by the terms, intel and money as an {@code ansi} code block. {@link DiscordFormat} colours mark out the
 * status line, badges and section headings; the figures themselves are left plain.</p>
 *
 * <p>Discord will not post a message over {@link #MAX_LENGTH} characters, and every colour code counts towards that,
 * so the block colours sparingly and uses plain resets. The briefing gets whatever room the block leaves, and is cut
 * short only when it will not fit.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractDiscordFormatter {
    /** The most characters Discord accepts in one message. */
    public static final int MAX_LENGTH = 2000;

    private static final String CODE_FENCE = "```";
    /** The widest a label column is padded to; a longer label simply runs into its value's column. */
    private static final int MAXIMUM_LABEL_WIDTH = 24;
    private static final String ELLIPSIS = "\u2026";
    /**
     * Clears every colour and effect. {@link DiscordFormat#RESET} also switches the text to white, which the block's
     * default colour already is, so this leaner reset saves characters on every coloured run.
     */
    private static final String RESET = "\u001b[0m";
    private static final Pattern MARKDOWN_SPECIALS = Pattern.compile("([\\\\*_~`|>#\\[\\]])");

    private ContractDiscordFormatter() {}

    /**
     * Formats an offer for Discord.
     *
     * @param readout the offer's readout
     *
     * @return the message, no longer than {@link #MAX_LENGTH}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String format(ContractReadout readout) {
        String header = "**" + escapeMarkdown(readout.title().toUpperCase(Locale.ROOT)) + "**\n"
                              + "-# " + escapeMarkdown(readout.employerLine()) + '\n';
        String speaker = readout.speaker() == null ? "" : "**" + escapeMarkdown(readout.speaker()) + "**\n";
        String block = codeBlock(readout);

        // The briefing gets whatever room the header and stat block leave.
        String briefing = quote(readout.briefing(),
              MAX_LENGTH - header.length() - speaker.length() - block.length());

        String message = header + (briefing.isEmpty() ? "" : speaker + briefing) + block;
        // A stat block alone over the limit cannot happen with the dossier's handful of rows, but never hand Discord
        // a message it will refuse.
        return message.length() <= MAX_LENGTH ? message
                     : message.substring(0, MAX_LENGTH - CODE_FENCE.length() - 1) + '\n' + CODE_FENCE;
    }

    private static String codeBlock(ContractReadout readout) {
        StringBuilder block = new StringBuilder(CODE_FENCE).append("ansi\n");
        block.append(DiscordFormat.CYAN).append(clean(readout.statusLine())).append(RESET).append('\n');
        block.append(clean(readout.assessment()));
        for (String badge : readout.badges()) {
            block.append(ContractReadout.separator()).append(DiscordFormat.PINK).append(clean(badge)).append(RESET);
        }
        block.append('\n');

        for (Section section : readout.sections()) {
            // Labels line up within a section, but one long label (such as the operating costs) does not pad them all.
            int labelWidth = 0;
            for (Row row : section.rows()) {
                labelWidth = Math.max(labelWidth, Math.min(MAXIMUM_LABEL_WIDTH, clean(row.label()).length()));
            }

            block.append('\n').append(DiscordFormat.BOLD).append(DiscordFormat.CYAN)
                  .append(clean(section.heading()).toUpperCase(Locale.ROOT)).append(RESET).append('\n');
            for (Row row : section.rows()) {
                block.append(pad(clean(row.label()), labelWidth)).append("  ").append(clean(row.value())).append('\n');
            }
            for (String note : section.notes()) {
                // Discord's only grey (DiscordFormat.GRAY) renders near-black on its dark theme, so notes keep the
                // block's own lighter text colour.
                block.append(clean(note)).append('\n');
            }
        }
        return block.append(CODE_FENCE).toString();
    }

    /**
     * Quotes the briefing, line by line, cutting it short (at a word, where it can) to fit {@code room} characters.
     *
     * @return the quoted briefing ending in a line break, or an empty string if there is no briefing or no room for it
     */
    static String quote(String briefing, int room) {
        String text = briefing == null ? "" : briefing.strip();
        if (text.isEmpty() || room <= 0) {
            return "";
        }

        String quoted = toQuote(escapeMarkdown(text));
        if (quoted.length() <= room) {
            return quoted;
        }

        // Shorten the plain text and re-quote, so the cut never splits an escape sequence or a quote marker.
        int keep = text.length();
        while (keep > 0) {
            keep = Math.min(keep - 1, (keep * room) / Math.max(1, quoted.length()));
            String shortened = shortenAtWord(text, keep) + ELLIPSIS;
            quoted = toQuote(escapeMarkdown(shortened));
            if (quoted.length() <= room) {
                return quoted;
            }
        }
        return "";
    }

    private static String toQuote(String text) {
        StringBuilder quoted = new StringBuilder();
        for (String line : text.split("\\R")) {
            quoted.append("> ").append(line).append('\n');
        }
        return quoted.toString();
    }

    private static String shortenAtWord(String text, int length) {
        if (length <= 0) {
            return "";
        }
        int cut = text.lastIndexOf(' ', length);
        return (cut > length / 2 ? text.substring(0, cut) : text.substring(0, length)).strip();
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    /** Escapes Discord Markdown, so a name or briefing reads as written rather than as formatting. */
    static String escapeMarkdown(String text) {
        return MARKDOWN_SPECIALS.matcher(text).replaceAll("\\\\$1");
    }

    /** Keeps text that goes inside the code block from closing it early. */
    private static String clean(String text) {
        return text.replace(CODE_FENCE, "'''").replace('\u001b', ' ');
    }
}

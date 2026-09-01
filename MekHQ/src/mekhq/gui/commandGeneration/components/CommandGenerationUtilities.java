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
package mekhq.gui.commandGeneration.components;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;

import java.util.Locale;

import megamek.common.annotations.Nullable;

/**
 * Shared helpers for the Command Generation dialog's styled components.
 *
 * <p>This is the Command Generation counterpart to {@code CampaignOptionsUtilities}. The two packages
 * share their visual conventions but each reads from its own resource bundle, so a key like
 * {@code "lblSparesArmor.text"} lives next to the dialog that uses it instead of polluting the larger
 * Campaign Options bundle.</p>
 */
public final class CommandGenerationUtilities {

    /**
     * Resource bundle holding all {@code lbl*.text} / {@code lbl*.tooltip} / {@code lbl*.border} keys
     * read by the styled components in this package. Mirrors the {@code mekhq.resources.CommandGenerationDialog}
     * properties file ({@code MekHQ/resources/mekhq/resources/CommandGenerationDialog.properties}).
     */
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CommandGenerationDialog";

    private static final String HTML_OPENING_TAG = "<html>";
    private static final String HTML_CLOSING_TAG = "</html>";

    private CommandGenerationUtilities() {
        // utility class
    }

    /**
     * Returns the resource-bundle identifier the styled components in this package read from. Use this
     * with {@link mekhq.utilities.MHQInternationalization#getTextAt(String, String)} the same way
     * {@code CampaignOptionsUtilities.getCampaignOptionsResourceBundle} is used in the Campaign Options
     * package.
     */
    public static String getCommandGenerationResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    /**
     * Word-wraps a bundle tooltip for display, tolerating text that is already wrapped in
     * {@code <html>} tags.
     *
     * <p>{@link megamek.client.ui.WrapLayout#wordWrap(String, int)} adds its own {@code <html>}
     * wrapper. Handing it a tooltip that already carries one - as any tooltip using {@code <br>} or
     * {@code <b>} must - leaves a stray {@code <html>} tag inside the document, and Swing's renderer
     * then silently drops everything from that tag to the next line break. The visible result is a
     * tooltip missing its opening line, or opening with a blank one. Stripping the outer tags here
     * keeps the markup inside the tooltip working while leaving exactly one wrapper on the result.</p>
     *
     * @param tooltipText    the tooltip text from the bundle, with or without surrounding
     *                       {@code <html>} tags; {@code null} is returned unchanged
     * @param customWrapSize maximum line length, or {@code null} for the default
     *
     * @return the wrapped tooltip, ready for {@code setToolTipText}
     */
    public static @Nullable String wrapTooltip(@Nullable String tooltipText,
          @Nullable Integer customWrapSize) {
        if (tooltipText == null) {
            return null;
        }
        return wordWrap(stripHtmlWrapper(tooltipText), processWrapSize(customWrapSize));
    }

    /**
     * Removes the outer {@code <html>} wrapper from a tooltip, if it has one.
     *
     * <p>The opening tag is removed when the text starts with it, and the closing tag when the text
     * ends with it, so a tooltip its author left unclosed is still unwrapped rather than kept with a
     * stray opening tag. Only the outermost tags are touched: markup inside the tooltip is left
     * alone, and text carrying no wrapper is returned unchanged.</p>
     *
     * @param text the text to unwrap
     *
     * @return the text with its outer {@code <html>} tags removed
     */
    private static String stripHtmlWrapper(String text) {
        String trimmed = text.trim();
        String lowerCase = trimmed.toLowerCase(Locale.ROOT);
        if (!lowerCase.startsWith(HTML_OPENING_TAG)) {
            return text;
        }
        trimmed = trimmed.substring(HTML_OPENING_TAG.length());
        if (lowerCase.endsWith(HTML_CLOSING_TAG)) {
            trimmed = trimmed.substring(0, trimmed.length() - HTML_CLOSING_TAG.length());
        }
        return trimmed;
    }
}

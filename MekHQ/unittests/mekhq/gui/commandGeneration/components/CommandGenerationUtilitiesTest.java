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

import static mekhq.gui.commandGeneration.components.CommandGenerationUtilities.getCommandGenerationResourceBundle;
import static mekhq.gui.commandGeneration.components.CommandGenerationUtilities.wrapTooltip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

/**
 * Guards the Command Generator's tooltips against the double-{@code <html>} defect reported in MekHQ
 * issue #9860.
 *
 * <p>A tooltip written with {@code <html>} tags so that it can use {@code <br>} used to be handed
 * straight to {@code WrapLayout.wordWrap}, which adds a wrapper of its own. The stray inner
 * {@code <html>} tag made Swing drop everything up to the next line break, so "Generate Captains"
 * opened with "lance)." and "Assign Best Officers" opened with a blank line.</p>
 */
class CommandGenerationUtilitiesTest {

    private static final String HTML_OPENING_TAG = "<html>";

    /**
     * The tooltip from the "Generate Captains" checkbox, which showed the defect most plainly: its
     * first line is 105 characters once the stray tag is counted, so the wrap fell just before
     * "lance)." and everything ahead of that was lost.
     */
    private static final String GENERATE_CAPTAINS_TOOLTIP =
          "<html>Creates a Captain for every company after the first (or every company when using a "
                + "mercenary company command lance).<br>Captains receive two officer skill increases.</html>";

    @Test
    void anAlreadyHtmlTooltipKeepsItsOpeningWords() {
        String wrapped = wrapTooltip(GENERATE_CAPTAINS_TOOLTIP, null);

        assertTrue(wrapped.startsWith("<html>Creates a Captain for every company"),
              "the opening of the tooltip must survive wrapping, got: " + wrapped);
    }

    @Test
    void anAlreadyHtmlTooltipIsWrappedExactlyOnce() {
        String wrapped = wrapTooltip(GENERATE_CAPTAINS_TOOLTIP, null);

        assertEquals(1, countOpeningHtmlTags(wrapped),
              "a second <html> tag inside the document is what made Swing drop the first line");
    }

    @Test
    void markupInsideTheTooltipIsLeftAlone() {
        String wrapped = wrapTooltip("<html>Costed at the <b>purchase cost</b>.<br>Not the resale value.</html>",
              null);

        assertTrue(wrapped.contains("<b>purchase cost</b>"), "inline markup must be preserved");
        assertTrue(wrapped.contains("Not the resale value."), "text after a line break must be preserved");
    }

    @Test
    void aPlainTooltipIsStillWrapped() {
        String wrapped = wrapTooltip("Generates 4 medics for every doctor created above.", null);

        assertEquals(1, countOpeningHtmlTags(wrapped));
        assertTrue(wrapped.startsWith("<html>Generates 4 medics"), "got: " + wrapped);
    }

    @Test
    void aCustomWrapSizeIsHonoured() {
        String wrapped = wrapTooltip("one two three four five", 8);

        assertTrue(wrapped.contains("<br>"), "a short wrap size must break the line, got: " + wrapped);
    }

    @Test
    void aNullTooltipStaysNull() {
        assertNull(wrapTooltip(null, null));
    }

    /**
     * Sweeps the live bundle so a tooltip added or translated later cannot reintroduce the defect: every
     * tooltip in the Command Generator must come out of the wrap with a single {@code <html>} tag and
     * with its own first word still in front.
     */
    @Test
    void everyTooltipInTheBundleSurvivesWrapping() {
        ResourceBundle bundle = ResourceBundle.getBundle(getCommandGenerationResourceBundle(), Locale.ROOT);

        int tooltipsChecked = 0;
        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (!key.endsWith(".tooltip")) {
                continue;
            }
            String tooltipText = bundle.getString(key).trim();
            if (tooltipText.isEmpty()) {
                continue;
            }
            tooltipsChecked++;

            String wrapped = wrapTooltip(tooltipText, null);
            assertEquals(1, countOpeningHtmlTags(wrapped), key + " must be wrapped in html exactly once");
            assertTrue(wrapped.startsWith(HTML_OPENING_TAG + firstWordOf(tooltipText)),
                  key + " lost its opening word; got: " + wrapped);
        }

        assertTrue(tooltipsChecked > 0, "the bundle should hold tooltips to check");
    }

    /**
     * @return the first word the reader should see, ignoring an {@code <html>} wrapper the author added
     */
    private static String firstWordOf(String tooltipText) {
        String text = tooltipText;
        if (text.toLowerCase(Locale.ROOT).startsWith(HTML_OPENING_TAG)) {
            text = text.substring(HTML_OPENING_TAG.length());
        }
        return text.trim().split("\\s+")[0];
    }

    /**
     * @return how many {@code <html>} opening tags the wrapped tooltip carries; the closing tag reads as
     *       {@code </html>} and is not counted
     */
    private static int countOpeningHtmlTags(String wrapped) {
        int count = 0;
        int index = wrapped.indexOf(HTML_OPENING_TAG);
        while (index >= 0) {
            count++;
            index = wrapped.indexOf(HTML_OPENING_TAG, index + HTML_OPENING_TAG.length());
        }
        return count;
    }
}

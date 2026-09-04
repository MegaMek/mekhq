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
package mekhq.gui.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import mekhq.gui.commandGeneration.components.CommandGenerationUtilities;
import mekhq.utilities.MHQInternationalization;
import org.junit.jupiter.api.Test;

/**
 * Every option in the Command Generator has help text in its bundle (MekHQ issue 9938).
 *
 * <p>The tabs build their controls from {@code lbl<name>.text} keys and read the matching
 * {@code lbl<name>.tooltip}; the two skill drop-downs and the tech assignment direction read a tooltip key of
 * their own. This checks the bundle rather than the built dialog: every labelled entry carries a tooltip that is
 * not blank and not the bundle's missing-key placeholder, apart from the few labels that are headings or body
 * text rather than options.</p>
 */
class CommandGenerationTooltipsTest {

    /** Labels that are not options: column headings and a paragraph of help, which explain nothing further. */
    private static final Set<String> NOT_OPTIONS = Set.of(
          "lblSupportPersonnelColumnRole",
          "lblSupportPersonnelColumnPercent",
          "lblSupportPersonnelColumnSkill",
          "lblSparesHelpBody");

    /** Tooltip keys read by controls that take their text from somewhere other than an {@code lbl} key. */
    private static final List<String> TOOLTIP_ONLY_KEYS = List.of(
          "lblAstechSkillLevel.tooltip",
          "lblMedicSkillLevel.tooltip",
          "cmbTechAssignmentDirection.tooltip",
          "supportCoveragePercent.toolTipText",
          "supportSkillLevel.toolTipText");

    private static ResourceBundle bundle() {
        return ResourceBundle.getBundle(CommandGenerationUtilities.getCommandGenerationResourceBundle());
    }

    @Test
    void everyLabelledOptionHasATooltip() {
        ResourceBundle bundle = bundle();
        Set<String> options = new TreeSet<>();
        for (String key : bundle.keySet()) {
            boolean isLabel = key.startsWith("lbl") && key.endsWith(".text");
            if (isLabel) {
                options.add(key.substring(0, key.length() - ".text".length()));
            }
        }
        options.removeAll(NOT_OPTIONS);
        assertFalse(options.isEmpty(), "the bundle names no options at all");

        Set<String> missing = new TreeSet<>();
        for (String option : options) {
            if (!hasHelp(bundle, option + ".tooltip")) {
                missing.add(option);
            }
        }
        assertTrue(missing.isEmpty(), missing.size() + " option(s) have no tooltip: " + missing);
    }

    @Test
    void controlsWithTheirOwnTooltipKeyHaveOne() {
        ResourceBundle bundle = bundle();
        Set<String> missing = new TreeSet<>();
        for (String key : TOOLTIP_ONLY_KEYS) {
            if (!hasHelp(bundle, key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "tooltip key(s) missing or blank: " + missing);
    }

    private static boolean hasHelp(ResourceBundle bundle, String key) {
        if (!bundle.containsKey(key)) {
            return false;
        }
        String text = bundle.getString(key);
        boolean isBlank = text.isBlank();
        boolean isPlaceholder = !MHQInternationalization.isResourceKeyValid(text);
        return !isBlank && !isPlaceholder;
    }
}

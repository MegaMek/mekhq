/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import static mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog.getFactionJudgmentDialogResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the ADOPTION_OR_MEKS out-of-character warning rendered by
 * {@code FactionAccoladeEvent}.
 *
 * <p>Guards against issue #8872, where the adoption.ooc string was fetched via
 * {@code getTextAt} (no {@code MessageFormat} substitution), causing a literal {@code {0}} to
 * appear in the dialog instead of the offering faction's name.</p>
 */
class FactionAccoladeEventAdoptionTextTest {
    private static final String ADOPTION_OOC_KEY =
          "FactionJudgmentDialog.message.ACCOLADE.ADOPTION_OR_MEKS.adoption.ooc";

    @Test
    void adoptionOocResourceContainsFactionNamePlaceholder() {
        String raw = getTextAt(getFactionJudgmentDialogResourceBundle(), ADOPTION_OOC_KEY);
        assertTrue(raw.contains("{0}"),
              "adoption.ooc resource string must contain a {0} placeholder for the faction name; "
                    + "update the test if the placeholder intentionally moves.");
    }

    @Test
    void adoptionOocFormattingSubstitutesFactionName() {
        String factionName = "Magistracy of Canopus";
        String formatted = getFormattedTextAt(
              getFactionJudgmentDialogResourceBundle(),
              ADOPTION_OOC_KEY,
              factionName);

        assertTrue(formatted.contains(factionName),
              "Formatted adoption.ooc must contain the faction name substituted for {0}.");
        assertFalse(formatted.contains("{0}"),
              "Formatted adoption.ooc must not contain a literal {0}; got: " + formatted);
    }
}

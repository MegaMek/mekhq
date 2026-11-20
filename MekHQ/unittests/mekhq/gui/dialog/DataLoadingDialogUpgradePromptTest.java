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
package mekhq.gui.dialog;

import static megamek.release.ReleaseChannel.CORE;
import static megamek.release.ReleaseChannel.EXPERIMENTAL;
import static mekhq.gui.dialog.DataLoadingDialog.isUpgradePromptRequired;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.Version;
import org.junit.jupiter.api.Test;

class DataLoadingDialogUpgradePromptTest {
    private static final Version CORE_BUILD = new Version("0.51.02.33");

    @Test
    void olderLinePromptsOnEveryChannel() {
        Version older = new Version("0.51.01.40");
        assertTrue(isUpgradePromptRequired(older, CORE, CORE_BUILD, CORE));
        assertTrue(isUpgradePromptRequired(older, EXPERIMENTAL, new Version("0.51.02.1"), EXPERIMENTAL));
        assertTrue(isUpgradePromptRequired(older, null, new Version("0.51.02"), null));
    }

    @Test
    void coreBuildPromptsForSameLineSaveNotWrittenByCore() {
        assertTrue(isUpgradePromptRequired(new Version("0.51.02.20"), EXPERIMENTAL, CORE_BUILD, CORE));
        assertTrue(isUpgradePromptRequired(new Version("0.51.02.20"), null, CORE_BUILD, CORE));
    }

    @Test
    void coreHotfixDoesNotPromptForEarlierCoreSave() {
        assertFalse(isUpgradePromptRequired(CORE_BUILD, CORE, new Version("0.51.02.34"), CORE));
        assertFalse(isUpgradePromptRequired(CORE_BUILD, CORE, CORE_BUILD, CORE));
    }

    @Test
    void nonCoreBuildsIgnoreSameLineRevisions() {
        assertFalse(isUpgradePromptRequired(new Version("0.51.03.1"), EXPERIMENTAL, new Version("0.51.03.2"),
              EXPERIMENTAL));
        assertFalse(isUpgradePromptRequired(new Version("0.51.03.1"), CORE, new Version("0.51.03.2"), null));
    }

    @Test
    void newerLineDoesNotPrompt() {
        assertFalse(isUpgradePromptRequired(new Version("0.51.03.1"), EXPERIMENTAL, CORE_BUILD, CORE));
    }
}

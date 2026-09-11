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
package mekhq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.NeuralInterfaceMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Covers remembering the augmentation rules the Command Generator was last used with.
 *
 * <p>The Setup tab shows a campaign its own settings, but a new campaign has none - both sit at
 * their all-off defaults - so without a remembered answer the same question had to be answered again
 * for every new campaign, and a player who forgot got a command with no augmentation and no way to
 * add it afterwards.</p>
 */
class MHQOptionsAugmentationTest {

    private final MHQOptions options = MekHQ.getMHQOptions();

    @AfterEach
    void restoreDefaults() {
        // These are real user preferences, so a test must not leave its answers behind.
        options.setLastUseImplants(false);
        options.setLastNeuralInterfaceMode(NeuralInterfaceMode.OFF);
    }

    /** Nothing remembered yet reads as the all-off defaults, which is what a first run should see. */
    @Test
    void nothingRememberedIsAllOff() {
        assertFalse(options.getLastUseImplants());
        assertEquals(NeuralInterfaceMode.OFF, options.getLastNeuralInterfaceMode());
    }

    @Test
    void eachAnswerIsRemembered() {
        options.setLastUseImplants(true);
        options.setLastNeuralInterfaceMode(NeuralInterfaceMode.FULL_TRACKING);

        assertTrue(options.getLastUseImplants());
        assertEquals(NeuralInterfaceMode.FULL_TRACKING, options.getLastNeuralInterfaceMode());
    }

    /** Every setting survives the round trip, the mode being stored by its option value. */
    @Test
    void everyNeuralInterfaceSettingIsRememberedExactly() {
        for (NeuralInterfaceMode mode : NeuralInterfaceMode.values()) {
            options.setLastNeuralInterfaceMode(mode);
            assertEquals(mode, options.getLastNeuralInterfaceMode());
        }
    }

    /** Turning a rule back off must be remembered too, not just turning it on. */
    @Test
    void turningARuleBackOffIsRemembered() {
        options.setLastUseImplants(true);
        options.setLastUseImplants(false);

        assertFalse(options.getLastUseImplants());
    }

    /** A null mode settles on the rules being off rather than throwing at a preference write. */
    @Test
    void aNullModeIsStoredAsOff() {
        options.setLastNeuralInterfaceMode(null);

        assertEquals(NeuralInterfaceMode.OFF, options.getLastNeuralInterfaceMode());
    }
}

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

package mekhq.campaign.unit;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Mothballing only records the unit's pre-mothball crew and formation when the save-mothball-state client option is
 * on. Cancelling mothballing with that option off used to crash trying to restore the record that was never made.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CancelMothballingTest {
    private static final int MOTHBALL_JOB_MINUTES = 960;

    private final PartsScenario scenario = PartsScenario.create();
    private Unit locust;

    @BeforeEach
    void setUp() {
        scenario.withAsTechs(6);
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        locust.setTech(scenario.withTech(EXP_REGULAR));
        // Mothballing under way, with no pre-mothball record, as when the option is off
        locust.setMothballTime(MOTHBALL_JOB_MINUTES);
    }

    @Test
    void cancellingWithoutASavedStateDoesNotCrash() {
        assertDoesNotThrow(locust::cancelMothballOrActivation);
    }

    @Test
    void cancellingWithoutASavedStateStopsTheMothballing() {
        locust.cancelMothballOrActivation();

        assertEquals(0, locust.getMothballTime());
        assertFalse(locust.isMothballed());
        assertFalse(locust.hasPendingMothballRestore());
    }
}

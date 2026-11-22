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
package mekhq.campaign.randomEvents.prisoners;

import static mekhq.campaign.randomEvents.prisoners.RecoverMIAPersonnel.SAR_CONTAINS_VTOL_OR_WIGE;
import static mekhq.campaign.randomEvents.prisoners.RecoverMIAPersonnel.SAR_HAS_IMPROVED_SENSORS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * Tests the target number for the daily rescue of Missing in Action personnel. The search modifiers make the search
 * easier, so they must lower the target number, as they do when capturing prisoners.
 */
class RecoverMIAPersonnelTest {
    private static final int BASE_TARGET_NUMBER = 8;

    private static RecoverMIAPersonnel recoveryWithQuality(int sarQuality) {
        Campaign campaign = mockCampaign();
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        return new RecoverMIAPersonnel(campaign, mock(Faction.class), sarQuality);
    }

    @Test
    void testBestQualityUsesImprovedSensors() {
        RecoverMIAPersonnel recovery = recoveryWithQuality(Integer.MAX_VALUE);

        assertEquals(BASE_TARGET_NUMBER + SAR_CONTAINS_VTOL_OR_WIGE + SAR_HAS_IMPROVED_SENSORS,
              recovery.getSarTargetNumber().getValue());
    }

    @Test
    void testWorstQualityHasNoSensors() {
        RecoverMIAPersonnel recovery = recoveryWithQuality(-1);

        assertEquals(BASE_TARGET_NUMBER + SAR_CONTAINS_VTOL_OR_WIGE, recovery.getSarTargetNumber().getValue());
    }
}

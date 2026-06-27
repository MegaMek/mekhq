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
package mekhq.campaign.universe.factionStanding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class FactionJudgmentTest {
    private static final LocalDate today = LocalDate.of(3151, 1, 1);

    @Test
    public void testFactionHasCensureWhenFactionHasCensureAboveMinimum() {
        FactionJudgment factionJudgment = new FactionJudgment();
        String factionCode = "FC_TEST";
        FactionCensureLevel censureLevel = FactionCensureLevel.CENSURE_LEVEL_2; // Above minimum severity

        Map<String, CensureEntry> censures = new HashMap<>();
        censures.put(factionCode, new CensureEntry(censureLevel, today));
        factionJudgment.setFactionCensures(censures);

        boolean result = factionJudgment.factionHasCensure(factionCode);

        assertTrue(result, "Expected faction to have a censure above the minimum severity");
    }

    @Test
    public void testFactionHasCensureWhenFactionHasCensureAtOrBelowMinimum() {
        FactionJudgment factionJudgment = new FactionJudgment();
        String factionCode = "FC_TEST";
        FactionCensureLevel censureLevel = FactionCensureLevel.CENSURE_LEVEL_0; // At minimum severity

        Map<String, CensureEntry> censures = new HashMap<>();
        censures.put(factionCode, new CensureEntry(censureLevel, today));
        factionJudgment.setFactionCensures(censures);

        boolean result = factionJudgment.factionHasCensure(factionCode);

        assertFalse(result, "Expected faction to not have a censure above the minimum severity");
    }

    @Test
    public void testFactionHasCensureWhenFactionHasNoCensure() {
        FactionJudgment factionJudgment = new FactionJudgment();
        String factionCode = "FC_TEST";

        Map<String, CensureEntry> censures = new HashMap<>();
        factionJudgment.setFactionCensures(censures); // Empty censures

        boolean result = factionJudgment.factionHasCensure(factionCode);

        assertFalse(result, "Expected faction with no censure entry to not have a censure above the minimum severity");
    }
}

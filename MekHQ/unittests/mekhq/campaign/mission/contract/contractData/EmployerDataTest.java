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
package mekhq.campaign.mission.contract.contractData;

import static megamek.client.ui.util.PlayerColour.BLUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import org.junit.jupiter.api.Test;

/**
 * Verifies the convenience and copy constructors of {@link EmployerData}, including the flavor/anchor/sponsor faction
 * split. Person, camouflage, and faction lookups are left as pass-through data so these tests need no campaign or
 * universe fixtures.
 */
class EmployerDataTest {

    private static EmployerData sampleEmployer(String sponsorFactionCode) {
        return new EmployerData(ChaosEmployerType.MERCENARY_SUBCONTRACT,
              "MERC",
              "FS",
              sponsorFactionCode,
              "Some Mercenary Command",
              null,
              null,
              null);
    }

    @Test
    void convenienceConstructorAppliesDefaultsAndKeepsFactionFields() {
        EmployerData employer = sampleEmployer(null);

        assertEquals(ChaosEmployerType.MERCENARY_SUBCONTRACT, employer.type());
        assertEquals("MERC", employer.factionCode(), "flavor faction is who pays the unit");
        assertEquals("FS", employer.anchorFactionCode(), "anchor faction situates the conflict geographically");
        assertNull(employer.sponsorFactionCode(), "an unsponsored employer has no patron");

        assertEquals(SkillLevel.REGULAR, employer.forceSkill(), "default force skill");
        assertEquals(DragoonRating.DRAGOON_C.getRating(), employer.equipmentRating(), "default equipment rating");
        assertEquals(BLUE, employer.color(), "employer forces default to the ally colour");
    }

    @Test
    void sponsorFactionCodeIsPreservedWhenPresent() {
        EmployerData employer = sampleEmployer("WOB");
        assertEquals("WOB", employer.sponsorFactionCode());
    }

    @Test
    void getSponsorFactionIsNullWhenThereIsNoSponsor() {
        // The null branch does not touch the Factions singleton, so it is safe to assert without a loaded universe.
        assertNull(sampleEmployer(null).getSponsorFaction());
    }

    @Test
    void copyConstructorOverridesRatingsAndPreservesEverythingElse() {
        EmployerData original = sampleEmployer("CS");

        EmployerData updated = new EmployerData(original, SkillLevel.VETERAN, 42);

        assertEquals(SkillLevel.VETERAN, updated.forceSkill(), "copy constructor sets the resolved force skill");
        assertEquals(42, updated.equipmentRating(), "copy constructor sets the resolved equipment rating");

        assertEquals(original.type(), updated.type());
        assertEquals(original.factionCode(), updated.factionCode());
        assertEquals(original.anchorFactionCode(), updated.anchorFactionCode());
        assertEquals(original.sponsorFactionCode(), updated.sponsorFactionCode());
        assertEquals(original.displayName(), updated.displayName());
        assertEquals(original.negotiator(), updated.negotiator());
        assertEquals(original.liaison(), updated.liaison());
        assertEquals(original.camouflage(), updated.camouflage());
        assertEquals(original.color(), updated.color());
    }
}

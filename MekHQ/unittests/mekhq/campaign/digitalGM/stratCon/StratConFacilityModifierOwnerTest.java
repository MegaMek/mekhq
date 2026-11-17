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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityDefinition;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A facility modifier whose force fights for the planet owner, such as Garrison Duty's concealed defences, fights for
 * whoever holds the facility instead. On a planet owned by neither the employer nor the enemy, the planet owner's side
 * would make it a third party: "Unknown Hostiles" sitting in the middle of the player's own base.
 */
class StratConFacilityModifierOwnerTest {
    private static final StratConFacilityDefinition MEK_BASE = new StratConFacilityDefinition("TestMekBase",
          "Mek Base",
          FacilityType.MekBase,
          null,
          null);

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static AtBScenarioModifier modifierWithForce(ForceAlignment alignment, boolean benefitsPlayer) {
        ScenarioForceTemplate force = new ScenarioForceTemplate();
        force.setForceAlignment(alignment.ordinal());

        AtBScenarioModifier modifier = new AtBScenarioModifier();
        modifier.setForceDefinition(force);
        modifier.setBenefitsPlayer(benefitsPlayer);
        return modifier;
    }

    private static ForceAlignment forceAlignment(AtBScenarioModifier modifier) {
        return ForceAlignment.getForceAlignment(modifier.getForceDefinition().getForceAlignment());
    }

    @ParameterizedTest
    @CsvSource({ "Allied, Allied, true", "Player, Allied, true", "Opposing, Opposing, false", "Third, Third, false" })
    void aPlanetOwnerForceFightsForTheFacilityOwner(ForceAlignment owner, ForceAlignment expectedSide,
          boolean expectedBenefitsPlayer) {
        AtBScenarioModifier modifier = modifierWithForce(ForceAlignment.PlanetOwner, !expectedBenefitsPlayer);

        StratConRulesManager.assignPlanetOwnerForceToFacilityOwner(modifier, new StratConFacility(MEK_BASE, owner));

        assertEquals(expectedSide, forceAlignment(modifier));
        assertEquals(expectedBenefitsPlayer, modifier.getBenefitsPlayer(),
              "restrictions on allied and enemy modifiers must see the side the force fights on");
    }

    @ParameterizedTest
    @CsvSource({ "Allied", "Opposing", "Third" })
    void aForceWithAFixedSideKeepsIt(ForceAlignment alignment) {
        AtBScenarioModifier modifier = modifierWithForce(alignment, false);

        StratConRulesManager.assignPlanetOwnerForceToFacilityOwner(modifier,
              new StratConFacility(MEK_BASE, ForceAlignment.Allied));

        assertEquals(alignment, forceAlignment(modifier));
        assertFalse(modifier.getBenefitsPlayer());
    }

    @Test
    void aFacilityWithoutAnOwnerLeavesThePlanetOwnerToDecide() {
        AtBScenarioModifier modifier = modifierWithForce(ForceAlignment.PlanetOwner, false);
        StratConFacility facility = new StratConFacility(MEK_BASE, ForceAlignment.Allied);
        facility.setOwner(null);

        StratConRulesManager.assignPlanetOwnerForceToFacilityOwner(modifier, facility);

        assertEquals(ForceAlignment.PlanetOwner, forceAlignment(modifier));
        assertFalse(modifier.getBenefitsPlayer());
    }

    @Test
    void aModifierWithoutAForceIsLeftAlone() {
        AtBScenarioModifier modifier = new AtBScenarioModifier();
        modifier.setBenefitsPlayer(true);

        StratConRulesManager.assignPlanetOwnerForceToFacilityOwner(modifier,
              new StratConFacility(MEK_BASE, ForceAlignment.Opposing));

        assertNull(modifier.getForceDefinition());
        assertTrue(modifier.getBenefitsPlayer());
    }
}

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A DropShip is maintained by its own crew, so its tech is the ship's engineer. The engineer must be chosen as soon as
 * crew come aboard or leave, whatever the special ability options are. Before this, the engineer was only chosen
 * again when abilities, Edge or implants were in use, so a DropShip crewed with those options off had no tech until
 * the next day (MekHQ issue 3474).
 */
class UnitEngineerCrewChangeTest {
    private PartsScenario scenario;
    private Unit leopard;

    private void setUp(boolean useSpecialAbilities) {
        scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.USE_ABILITIES, useSpecialAbilities);
        options.set(CampaignOption.USE_EDGE, useSpecialAbilities);
        options.set(CampaignOption.USE_IMPLANTS, useSpecialAbilities);
        leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
    }

    private Person boardVesselCrew() {
        Person crewMember = scenario.withTech(EXP_REGULAR);
        crewMember.setPrimaryRoleDirect(PersonnelRole.VESSEL_CREW);
        leopard.addVesselCrew(crewMember);
        return crewMember;
    }

    @ParameterizedTest(name = "special abilities in use: {0}")
    @ValueSource(booleans = { false, true })
    void crewComingAboardGivesTheShipATech(boolean useSpecialAbilities) {
        setUp(useSpecialAbilities);

        Person crewMember = boardVesselCrew();

        assertSame(crewMember, leopard.getEngineer());
        assertSame(crewMember, leopard.getTech());
        assertTrue(crewMember.isEngineer());
    }

    @ParameterizedTest(name = "special abilities in use: {0}")
    @ValueSource(booleans = { false, true })
    void engineerLeavingHandsTheShipToTheRemainingCrew(boolean useSpecialAbilities) {
        setUp(useSpecialAbilities);
        boardVesselCrew();
        boardVesselCrew();
        Person firstEngineer = leopard.getEngineer();
        assertNotNull(firstEngineer, "The crewed Leopard has an engineer");

        leopard.remove(firstEngineer, true);

        Person remainingCrewMember = leopard.getVesselCrew().getFirst();
        assertSame(remainingCrewMember, leopard.getEngineer());
        assertSame(remainingCrewMember, leopard.getTech());
        assertFalse(firstEngineer.isEngineer());
        // Leaving the ship only unassigns the old engineer; they stay in the campaign
        assertTrue(scenario.getCampaign().getPlayerForce().getHumanResources().getActivePersonnel(false, false)
                         .contains(firstEngineer));
    }
}

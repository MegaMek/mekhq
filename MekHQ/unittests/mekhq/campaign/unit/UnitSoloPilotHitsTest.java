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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import megamek.common.units.Crew;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A Mek's pilot takes their current injuries into battle as crew hits, and only those. Before, the hits were only set
 * when abilities, Edge or implants were in use; with those options off, the hits from the last battle stayed on the
 * unit, so a healed pilot still fought hurt and the unit could stay crippled (MegaMek issue 9719).
 */
class UnitSoloPilotHitsTest {
    private static final int PILOT_SKILL_LEVEL = 4;
    private static final int PILOT_INJURY_HITS = 2;
    private static final int HITS_FROM_THE_LAST_BATTLE = 5;

    private Unit wolverine;
    private Person pilot;

    private void setUp(boolean useSpecialAbilities) {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.USE_ABILITIES, useSpecialAbilities);
        options.set(CampaignOption.USE_EDGE, useSpecialAbilities);
        options.set(CampaignOption.USE_IMPLANTS, useSpecialAbilities);
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);

        if (SkillType.lookupHash == null) {
            SkillType.initializeTypes();
        }
        pilot = new Person("Test", "Pilot", campaign);
        pilot.setPrimaryRoleDirect(PersonnelRole.MEKWARRIOR);
        pilot.addSkill(SkillType.S_PILOT_MEK, PILOT_SKILL_LEVEL, 0);
        pilot.addSkill(SkillType.S_GUN_MEK, PILOT_SKILL_LEVEL, 0);
        // Imported the way the campaign imports a person, so the pilot stands at the main force like its units
        campaign.importPerson(pilot);
        wolverine.addPilotOrSoldier(pilot);
    }

    private Crew crew() {
        return wolverine.getEntity().getCrew();
    }

    @ParameterizedTest(name = "special abilities in use: {0}")
    @ValueSource(booleans = { false, true })
    void injuredPilotFightsWithTheirInjuries(boolean useSpecialAbilities) {
        setUp(useSpecialAbilities);
        pilot.setHits(PILOT_INJURY_HITS);

        wolverine.resetPilotAndEntity();

        assertEquals(PILOT_INJURY_HITS, crew().getHits());
    }

    @ParameterizedTest(name = "special abilities in use: {0}")
    @ValueSource(booleans = { false, true })
    void healedPilotLeavesTheLastBattlesHitsBehind(boolean useSpecialAbilities) {
        setUp(useSpecialAbilities);
        crew().setHits(HITS_FROM_THE_LAST_BATTLE, 0);
        pilot.setHits(0);

        wolverine.resetPilotAndEntity();

        assertEquals(0, crew().getHits());
        assertFalse(wolverine.getEntity().isCrippled(), "A repaired Mek with a healed pilot is not crippled");
    }

    @ParameterizedTest(name = "special abilities in use: {0}")
    @ValueSource(booleans = { false, true })
    void pilotWhoSurvivedBeingDoomedComesBackWithTheirInjuries(boolean useSpecialAbilities) {
        setUp(useSpecialAbilities);
        // A pilot doomed in battle has every slot set to 6 hits; MekHQ decides afterwards whether they really died
        crew().setDoomed(true);
        pilot.setHits(PILOT_INJURY_HITS);

        wolverine.resetPilotAndEntity();

        assertEquals(PILOT_INJURY_HITS, crew().getHits());
        assertFalse(crew().isDead(), "The surviving pilot is not left dead on the unit");
    }
}

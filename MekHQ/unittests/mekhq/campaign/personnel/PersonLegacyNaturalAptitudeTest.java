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
package mekhq.campaign.personnel;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the conversion of the retired Natural Aptitude SPAs into per-skill Natural Aptitudes when an older campaign is
 * loaded.
 */
class PersonLegacyNaturalAptitudeTest {
    private Campaign campaign;
    private Person person;

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn("MERC");

        person = spy(new Person(campaign));
        doReturn("Test Person").when(person).getHyperlinkedFullTitle();
    }

    private boolean hasNaturalAptitude(String skillName) {
        return person.getSkill(skillName).getHasNaturalAptitude();
    }

    @Test
    void gunnerySpaGivesEveryCombatGunnerySkillAnAptitude() {
        person.addSkill(SkillType.S_GUN_MEK, 3, 0);
        person.addSkill(SkillType.S_SMALL_ARMS, 3, 0);
        person.addSkill(SkillType.S_MARTIAL_ARTS, 3, 0);
        person.addSkill(SkillType.S_PILOT_MEK, 3, 0);
        person.addSkill(SkillType.S_ADMIN, 3, 0);

        person.convertLegacyNaturalAptitudes(true, false);

        assertTrue(hasNaturalAptitude(SkillType.S_GUN_MEK));
        assertTrue(hasNaturalAptitude(SkillType.S_SMALL_ARMS));
        assertTrue(hasNaturalAptitude(SkillType.S_MARTIAL_ARTS));
        assertFalse(hasNaturalAptitude(SkillType.S_PILOT_MEK), "piloting skills don't get the gunnery aptitude");
        assertFalse(hasNaturalAptitude(SkillType.S_ADMIN), "non-combat skills don't get an aptitude");
    }

    @Test
    void pilotingSpaGivesEveryCombatPilotingSkillAnAptitude() {
        person.addSkill(SkillType.S_PILOT_MEK, 3, 0);
        person.addSkill(SkillType.S_PILOT_AERO, 3, 0);
        person.addSkill(SkillType.S_ANTI_MEK, 3, 0);
        person.addSkill(SkillType.S_GUN_MEK, 3, 0);

        person.convertLegacyNaturalAptitudes(false, true);

        assertTrue(hasNaturalAptitude(SkillType.S_PILOT_MEK));
        assertTrue(hasNaturalAptitude(SkillType.S_PILOT_AERO));
        assertTrue(hasNaturalAptitude(SkillType.S_ANTI_MEK));
        assertFalse(hasNaturalAptitude(SkillType.S_GUN_MEK), "gunnery skills don't get the piloting aptitude");
    }

    @Test
    void bothSpasConvertTogether() {
        person.addSkill(SkillType.S_GUN_MEK, 3, 0);
        person.addSkill(SkillType.S_PILOT_MEK, 3, 0);

        person.convertLegacyNaturalAptitudes(true, true);
        person.reportUnresolvedLegacyNaturalAptitudes(campaign);

        assertTrue(hasNaturalAptitude(SkillType.S_GUN_MEK));
        assertTrue(hasNaturalAptitude(SkillType.S_PILOT_MEK));
        verify(campaign, never()).addReport(eq(GENERAL), anyString());
    }

    @Test
    void conversionIsFree() {
        person.addSkill(SkillType.S_GUN_MEK, 3, 0);
        person.setXPDirect(100);

        person.convertLegacyNaturalAptitudes(true, true);

        assertEquals(100, person.getXP(), "converting the old SPAs costs no XP");
    }

    @Test
    void noSpasChangeNothingAndReportNothing() {
        person.addSkill(SkillType.S_GUN_MEK, 3, 0);

        person.convertLegacyNaturalAptitudes(false, false);
        person.reportUnresolvedLegacyNaturalAptitudes(campaign);

        assertFalse(hasNaturalAptitude(SkillType.S_GUN_MEK));
        verify(campaign, never()).addReport(eq(GENERAL), anyString());
    }

    @Test
    void gunnerySpaWithoutGunnerySkillsIsReported() {
        person.addSkill(SkillType.S_PILOT_MEK, 3, 0);

        person.convertLegacyNaturalAptitudes(true, false);
        person.reportUnresolvedLegacyNaturalAptitudes(campaign);

        verify(campaign, times(1)).addReport(eq(GENERAL),
              argThat(report -> report.contains("Test Person") && report.contains("Gunnery")));
        assertFalse(hasNaturalAptitude(SkillType.S_PILOT_MEK), "the gunnery aptitude doesn't move to piloting");
    }

    @Test
    void pilotingSpaWithoutPilotingSkillsIsReported() {
        person.addSkill(SkillType.S_GUN_MEK, 3, 0);

        person.convertLegacyNaturalAptitudes(false, true);
        person.reportUnresolvedLegacyNaturalAptitudes(campaign);

        verify(campaign, times(1)).addReport(eq(GENERAL),
              argThat(report -> report.contains("Test Person") && report.contains("Piloting")));
    }

    @Test
    void bothUnresolvedSpasAreReportedSeparately() {
        person.addSkill(SkillType.S_ADMIN, 3, 0);

        person.convertLegacyNaturalAptitudes(true, true);
        person.reportUnresolvedLegacyNaturalAptitudes(campaign);

        verify(campaign, times(2)).addReport(eq(GENERAL), anyString());
    }

    @Test
    void unresolvedSpasAreOnlyReportedOnce() {
        person.convertLegacyNaturalAptitudes(true, false);

        person.reportUnresolvedLegacyNaturalAptitudes(campaign);
        person.reportUnresolvedLegacyNaturalAptitudes(campaign);

        verify(campaign, times(1)).addReport(eq(GENERAL), anyString());
    }

    @Test
    void oneResolvedAndOneUnresolvedOnlyReportsTheUnresolved() {
        person.addSkill(SkillType.S_GUN_MEK, 3, 0);

        person.convertLegacyNaturalAptitudes(true, true);
        person.reportUnresolvedLegacyNaturalAptitudes(campaign);

        assertTrue(hasNaturalAptitude(SkillType.S_GUN_MEK));
        verify(campaign, times(1)).addReport(eq(GENERAL), argThat(report -> report.contains("Piloting")));
    }
}

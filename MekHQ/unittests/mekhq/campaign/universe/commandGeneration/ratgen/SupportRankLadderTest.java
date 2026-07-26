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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * Verifies the skill-linked rank ladder for generated support staff.
 *
 * <p>Every support person previously received one rank index regardless of role or skill, which in the
 * shipped rank systems renders as Corporal - so a command's chief surgeon and its greenest astech
 * ranked identically. These tests pin the ladder that replaced it.</p>
 */
class SupportRankLadderTest {

    private static Faction innerSphereFaction() {
        Faction faction = mock(Faction.class);
        when(faction.isClan()).thenReturn(false);
        when(faction.isComStarOrWoB()).thenReturn(false);
        return faction;
    }

    private static Faction clanFaction() {
        Faction faction = mock(Faction.class);
        when(faction.isClan()).thenReturn(true);
        return faction;
    }

    private static int rankFor(PersonnelRole role, SkillLevel skill) {
        return RulesetRankAssigner.supportRankFor(role, skill, innerSphereFaction());
    }

    @Test
    void doctorRankRisesWithSkill() {
        int green = rankFor(PersonnelRole.DOCTOR, SkillLevel.GREEN);
        int regular = rankFor(PersonnelRole.DOCTOR, SkillLevel.REGULAR);
        int veteran = rankFor(PersonnelRole.DOCTOR, SkillLevel.VETERAN);
        int elite = rankFor(PersonnelRole.DOCTOR, SkillLevel.ELITE);

        assertTrue(green < regular, "a regular doctor should outrank a green one");
        assertTrue(regular < veteran, "a veteran doctor should outrank a regular one");
        assertTrue(veteran < elite, "an elite doctor should outrank a veteran one");
    }

    @Test
    void doctorsAreCommissioned() {
        // The reported problem: every doctor arrived as a Corporal, an enlisted rank. Physicians hold
        // commissions in the services this ladder is modelled on.
        for (SkillLevel skill : new SkillLevel[] { SkillLevel.GREEN, SkillLevel.REGULAR,
                                                   SkillLevel.VETERAN, SkillLevel.ELITE,
                                                   SkillLevel.LEGENDARY }) {
            assertTrue(rankFor(PersonnelRole.DOCTOR, skill) >= Rank.RO_MIN,
                  skill + " doctors should hold a commissioned rank");
        }
    }

    @Test
    void administratorsAreCommissionedAndTechniciansAreNot() {
        assertTrue(rankFor(PersonnelRole.ADMINISTRATOR_COMMAND, SkillLevel.REGULAR) >= Rank.RO_MIN,
              "administrators should be commissioned");
        for (PersonnelRole role : new PersonnelRole[] { PersonnelRole.MEK_TECH, PersonnelRole.MECHANIC,
                                                        PersonnelRole.AERO_TEK, PersonnelRole.BA_TECH,
                                                        PersonnelRole.ASTECH, PersonnelRole.MEDIC }) {
            assertTrue(rankFor(role, SkillLevel.ELITE) <= Rank.RE_MAX,
                  role + " should stay in the enlisted band");
        }
    }

    @Test
    void commissionedRanksStopBelowColonel() {
        // Capped on purpose: a command's surgeon should not outrank the officer commanding it. Colonel
        // sits at index 38 in the shipped systems.
        int colonel = 38;
        for (SkillLevel skill : new SkillLevel[] { SkillLevel.ELITE, SkillLevel.HEROIC,
                                                   SkillLevel.LEGENDARY }) {
            assertTrue(rankFor(PersonnelRole.DOCTOR, skill) < colonel,
                  skill + " doctors should rank below Colonel");
        }
    }

    @Test
    void exceptionalStaffShareTheTopOfTheirBand() {
        // Heroic and Legendary are rare enough that giving each its own rung would add nothing; both
        // sit at the cap.
        assertEquals(rankFor(PersonnelRole.DOCTOR, SkillLevel.ELITE),
              rankFor(PersonnelRole.DOCTOR, SkillLevel.LEGENDARY));
        assertEquals(rankFor(PersonnelRole.MEK_TECH, SkillLevel.ELITE),
              rankFor(PersonnelRole.MEK_TECH, SkillLevel.LEGENDARY));
    }

    @Test
    void technicianRankRisesWithSkillWithinTheEnlistedBand() {
        int green = rankFor(PersonnelRole.MEK_TECH, SkillLevel.GREEN);
        int regular = rankFor(PersonnelRole.MEK_TECH, SkillLevel.REGULAR);
        int elite = rankFor(PersonnelRole.MEK_TECH, SkillLevel.ELITE);

        assertTrue(green < regular, "a regular tech should outrank a green one");
        assertTrue(regular < elite, "an elite tech should outrank a regular one");
        assertTrue(elite <= Rank.RE_MAX, "technicians stay enlisted");
    }

    @Test
    void clanAndComStarKeepTheirExistingFlatRank() {
        // Neither organises medical or technical staff as a commissioned corps, so the ladder is not
        // imposed on them; that is a separate piece of work.
        int clanDoctor = RulesetRankAssigner.supportRankFor(PersonnelRole.DOCTOR, SkillLevel.ELITE,
              clanFaction());
        int clanTech = RulesetRankAssigner.supportRankFor(PersonnelRole.MEK_TECH, SkillLevel.GREEN,
              clanFaction());
        assertEquals(clanDoctor, clanTech, "Clan support staff keep one flat index for now");
    }

    @Test
    void anUnknownSkillLevelStillProducesASensibleRank() {
        // Defensive: null should not blow up or produce rank 0.
        assertTrue(RulesetRankAssigner.supportRankFor(PersonnelRole.DOCTOR, null,
              innerSphereFaction()) >= Rank.RO_MIN);
        assertTrue(RulesetRankAssigner.supportRankFor(PersonnelRole.MEK_TECH, null,
              innerSphereFaction()) > 0);
    }
}

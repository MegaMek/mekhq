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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

/**
 * Verifies the skill-linked rank ladder for generated support staff.
 *
 * <p>Every support person previously received one rank index regardless of role or skill, which in the
 * shipped rank systems renders as Corporal - so a command's chief surgeon and its greenest astech
 * ranked identically. These tests pin the ladder that replaced it.</p>
 *
 * <p>Commissioned staff stop at Captain on purpose. Field grade belongs to the posts
 * {@link SeniorAppointmentAssigner} fills, not to raw skill: the generator creates a whole cohort at
 * one experience level, so mapping skill straight onto seniority turned every doctor in a Veteran
 * command into a Major.</p>
 */
class SupportRankLadderTest {

    @BeforeAll
    static void loadRankSystems() {
        Ranks.initializeRankSystems();
    }

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
        int veteran = rankFor(PersonnelRole.DOCTOR, SkillLevel.VETERAN);

        assertTrue(green < veteran, "a veteran doctor should outrank a green one");
    }

    @Test
    void commissionedStaffStopAtCaptainWhateverTheirSkill() {
        // The reported problem: a command generated at Veteran gave every doctor and administrator a
        // field-grade rank, because the whole cohort shares one experience level. Field grade is now
        // reserved for the heads the appointment pass promotes.
        int captain = 34;
        for (SkillLevel skill : new SkillLevel[] { SkillLevel.GREEN, SkillLevel.REGULAR,
                                                   SkillLevel.VETERAN, SkillLevel.ELITE,
                                                   SkillLevel.HEROIC, SkillLevel.LEGENDARY }) {
            assertTrue(rankFor(PersonnelRole.DOCTOR, skill) <= captain,
                  skill + " doctors should be no higher than Captain");
            assertTrue(rankFor(PersonnelRole.ADMINISTRATOR, skill) <= captain,
                  skill + " administrators should be no higher than Captain");
        }
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
        assertTrue(rankFor(PersonnelRole.ADMINISTRATOR, SkillLevel.REGULAR) >= Rank.RO_MIN,
              "administrators should be commissioned");
        for (PersonnelRole role : new PersonnelRole[] { PersonnelRole.MEK_TECH, PersonnelRole.MECHANIC,
                                                        PersonnelRole.AERO_TEK, PersonnelRole.BA_TECH,
                                                        PersonnelRole.ASTECH, PersonnelRole.MEDIC }) {
            assertTrue(rankFor(role, SkillLevel.ELITE) <= Rank.RE_MAX,
                  role + " should stay in the enlisted band");
        }
    }

    @Test
    void commissionedRanksStayBelowFieldGrade() {
        // Major sits at 35. Nothing the skill ladder produces should reach it.
        int major = 35;
        for (SkillLevel skill : new SkillLevel[] { SkillLevel.ELITE, SkillLevel.HEROIC,
                                                   SkillLevel.LEGENDARY }) {
            assertTrue(rankFor(PersonnelRole.DOCTOR, skill) < major,
                  skill + " doctors should rank below Major until they hold a post");
        }
    }

    @Test
    void exceptionalStaffShareTheTopOfTheirBand() {
        // Heroic and Legendary are rare enough that giving each its own rung would add nothing; both
        // sit at the cap.
        assertEquals(rankFor(PersonnelRole.DOCTOR, SkillLevel.ELITE),
              rankFor(PersonnelRole.DOCTOR, SkillLevel.LEGENDARY));
        assertEquals(rankFor(PersonnelRole.DOCTOR, SkillLevel.VETERAN),
              rankFor(PersonnelRole.DOCTOR, SkillLevel.ELITE),
              "Veteran and above share Captain");
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

    /**
     * The ladder picks an index; no rank system is obliged to fill it. SLDF leaves 16 and 37 blank in
     * every profession column, so a support person handed one of those rendered with a bare "-" where
     * their rank should be. The generator must walk down to a rung that system actually names.
     */
    @Test
    void anIndexTheRankSystemLeavesBlankFallsBackToARealRank() {
        RankSystem sldf = Ranks.getRankSystems().get("SLDF");
        assertNotNull(sldf, "the shipped rank data should contain SLDF");

        for (int blankIndex : new int[] { 16, 37 }) {
            Rank blank = sldf.getRank(blankIndex);
            assertNotNull(blank);
            assertTrue(blank.isEmpty(Profession.MEDICAL),
                  "index " + blankIndex + " is expected to be blank in SLDF; if the data changed, "
                        + "this test needs a different index rather than deleting");

            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(PersonnelRole.DOCTOR);
            when(person.getRankSystem()).thenReturn(sldf);
            when(person.getFullName()).thenReturn("Test Subject");

            RulesetRankAssigner.setRankWithFallback(person, blankIndex, sldf, mock(RankValidator.class));

            ArgumentCaptor<Integer> assigned = ArgumentCaptor.forClass(Integer.class);
            verify(person, atLeastOnce()).setRank(assigned.capture());
            int finalRank = assigned.getAllValues().get(assigned.getAllValues().size() - 1);

            assertTrue(finalRank < blankIndex,
                  "a blank rung should step down, but index " + blankIndex + " stayed put");
            assertFalse(sldf.getRank(finalRank).isEmpty(
                        Profession.MEDICAL.getProfession(sldf, sldf.getRank(finalRank))),
                  "the chosen rank should render a real name, not \"-\"");
        }
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

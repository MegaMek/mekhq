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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link Unit} hands each person's Natural Aptitudes, and the skill they fight with on foot, to the
 * MegaMek crew.
 */
class UnitNaturalAptitudeTest {
    private static final String GUN_TYPE = SkillType.S_GUN_MEK;
    private static final String DRIVE_TYPE = SkillType.S_PILOT_MEK;

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Entity entity;
    private Unit unit;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        setUseArtillery(false);
        setUseSmallArmsOnly(false);
        setUseAdvancedMedical(false);

        entity = mock(Entity.class);
        unit = new Unit(entity, campaign);
    }

    private void setUseArtillery(boolean isUseArtillery) {
        lenient().when(campaignOptions.get(CampaignOption.USE_ARTILLERY)).thenReturn(isUseArtillery);
    }

    private void setUseSmallArmsOnly(boolean isUseSmallArmsOnly) {
        lenient().when(campaignOptions.get(CampaignOption.USE_SMALL_ARMS_ONLY)).thenReturn(isUseSmallArmsOnly);
    }

    private void setUseAdvancedMedical(boolean isUseAdvancedMedical) {
        lenient().when(campaignOptions.isUseAdvancedMedical()).thenReturn(isUseAdvancedMedical);
    }

    private void setConventionalInfantry(boolean isConventionalInfantry) {
        lenient().when(entity.isConventionalInfantry()).thenReturn(isConventionalInfantry);
    }

    /**
     * Gives a person a skill.
     *
     * @param level              the skill's total level, used to pick the best infantry weapon skill
     * @param finalValue         the skill's final value (target number), as sent to MegaMek
     * @param hasNaturalAptitude whether the person has a Natural Aptitude in the skill
     */
    private static Skill giveSkill(Person person, String skillName, int level, int finalValue,
          boolean hasNaturalAptitude) {
        Skill skill = mock(Skill.class);
        lenient().when(skill.getHasNaturalAptitude()).thenReturn(hasNaturalAptitude);
        lenient().when(skill.getTotalSkillLevel(any())).thenReturn(level);
        lenient().when(skill.getFinalSkillValue(any())).thenReturn(finalValue);
        lenient().when(skill.getFinalSkillValue(any(), anyInt())).thenReturn(finalValue);
        lenient().when(person.hasSkill(skillName)).thenReturn(true);
        lenient().when(person.getSkill(skillName)).thenReturn(skill);
        return skill;
    }

    private static Skill giveSkill(Person person, String skillName, boolean hasNaturalAptitude) {
        return giveSkill(person, skillName, 3, 4, hasNaturalAptitude);
    }

    private static Crew crewWithEveryAptitude() {
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setHasNaturalAptitudeGunnery(true, 0);
        crew.setHasNaturalAptitudeArtillery(true, 0);
        crew.setHasNaturalAptitudePiloting(true, 0);
        crew.setHasNaturalAptitudeSmallArms(true, 0);
        return crew;
    }

    @Nested
    class GunneryAndPiloting {
        @Test
        void aptitudesComeFromTheUnitsSkills() {
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, true);
            giveSkill(person, DRIVE_TYPE, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertTrue(crew.isHasNaturalAptitudeGunnery(0));
            assertTrue(crew.isHasNaturalAptitudePiloting(0));
        }

        @Test
        void eachAptitudeIsIndependent() {
            Person gunner = mock(Person.class);
            giveSkill(gunner, GUN_TYPE, true);
            giveSkill(gunner, DRIVE_TYPE, false);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, gunner, GUN_TYPE, DRIVE_TYPE);

            assertTrue(crew.isHasNaturalAptitudeGunnery(0));
            assertFalse(crew.isHasNaturalAptitudePiloting(0));
        }

        @Test
        void skillsWithoutAptitudeGiveNone() {
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, false);
            giveSkill(person, DRIVE_TYPE, false);
            Crew crew = crewWithEveryAptitude();

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
            assertFalse(crew.isHasNaturalAptitudePiloting(0));
        }

        @Test
        void missingSkillsGiveNoAptitudeRatherThanFailing() {
            Crew crew = crewWithEveryAptitude();

            unit.updateCrewNaturalAptitudes(crew, 0, mock(Person.class), GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
            assertFalse(crew.isHasNaturalAptitudeArtillery(0));
            assertFalse(crew.isHasNaturalAptitudePiloting(0));
        }

        @Test
        void aptitudeInADifferentSkillDoesNotCount() {
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_GUN_AERO, true);
            giveSkill(person, SkillType.S_PILOT_AERO, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0), "a Mek's gunnery skill is Gunnery/Mek");
            assertFalse(crew.isHasNaturalAptitudePiloting(0), "a Mek's piloting skill is Piloting/Mek");
        }

        @Test
        void noPersonClearsThePreviousOccupantsAptitudes() {
            Crew crew = crewWithEveryAptitude();

            unit.updateCrewNaturalAptitudes(crew, 0, null, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
            assertFalse(crew.isHasNaturalAptitudeArtillery(0));
            assertFalse(crew.isHasNaturalAptitudePiloting(0));
        }

        @Test
        void onlyTheGivenSlotIsUpdated() {
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, true);
            giveSkill(person, DRIVE_TYPE, true);
            Crew crew = new Crew(CrewType.TRIPOD);

            unit.updateCrewNaturalAptitudes(crew, 1, person, GUN_TYPE, DRIVE_TYPE);

            assertTrue(crew.isHasNaturalAptitudeGunnery(1));
            assertTrue(crew.isHasNaturalAptitudePiloting(1));
            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
            assertFalse(crew.isHasNaturalAptitudePiloting(0));
        }

        @Test
        void smallArmsAptitudeIsNotSetHere() {
            // The Small Arms aptitude is set alongside its skill by updateCrewSmallArms, not here
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
        }
    }

    @Nested
    class Artillery {
        @Test
        void withTheArtillerySkillTheArtilleryAptitudeIsUsed() {
            setUseArtillery(true);
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, false);
            giveSkill(person, SkillType.S_ARTILLERY, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertTrue(crew.isHasNaturalAptitudeArtillery(0));
            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
        }

        @Test
        void withTheArtillerySkillGunneryAptitudeDoesNotCoverArtillery() {
            setUseArtillery(true);
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, true);
            giveSkill(person, SkillType.S_ARTILLERY, false);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeArtillery(0));
            assertTrue(crew.isHasNaturalAptitudeGunnery(0));
        }

        @Test
        void withTheArtillerySkillButNoArtillerySkillThereIsNoArtilleryAptitude() {
            setUseArtillery(true);
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeArtillery(0));
        }

        @Test
        void withoutTheArtillerySkillArtilleryFollowsGunnery() {
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertTrue(crew.isHasNaturalAptitudeArtillery(0), "artillery is fired with gunnery");
        }

        @Test
        void withoutTheArtillerySkillAnArtilleryAptitudeIsIgnored() {
            Person person = mock(Person.class);
            giveSkill(person, GUN_TYPE, false);
            giveSkill(person, SkillType.S_ARTILLERY, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, person, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeArtillery(0));
        }
    }

    @Nested
    class ConventionalInfantry {
        private Person soldier;

        @BeforeEach
        void setUp() {
            setConventionalInfantry(true);
            soldier = mock(Person.class);
            // Support Weapons is their best infantry skill, and the only one with an aptitude
            giveSkill(soldier, SkillType.S_SMALL_ARMS, 2, 5, false);
            giveSkill(soldier, SkillType.S_SUPPORT_WEAPONS, 6, 3, true);
        }

        @Test
        void gunneryAptitudeComesFromTheBestInfantrySkill() {
            Crew crew = new Crew(CrewType.INFANTRY_CREW);

            unit.updateCrewNaturalAptitudes(crew, 0, soldier, SkillType.S_SMALL_ARMS, SkillType.S_ANTI_MEK);

            assertTrue(crew.isHasNaturalAptitudeGunnery(0));
        }

        @Test
        void withSmallArmsOnlyTheGunneryAptitudeComesFromSmallArms() {
            setUseSmallArmsOnly(true);
            Crew crew = new Crew(CrewType.INFANTRY_CREW);

            unit.updateCrewNaturalAptitudes(crew, 0, soldier, SkillType.S_SMALL_ARMS, SkillType.S_ANTI_MEK);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
        }

        @Test
        void soldierWithNoInfantrySkillHasNoGunneryAptitude() {
            Crew crew = crewWithEveryAptitude();

            unit.updateCrewNaturalAptitudes(crew, 0, mock(Person.class), SkillType.S_SMALL_ARMS,
                  SkillType.S_ANTI_MEK);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
        }

        @Test
        void otherUnitsIgnoreInfantrySkills() {
            setConventionalInfantry(false);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewNaturalAptitudes(crew, 0, soldier, GUN_TYPE, DRIVE_TYPE);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
        }
    }

    @Nested
    class SkillOnFoot {
        @Test
        void smallArmsSkillAndAptitudeAreSent() {
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 3, 4, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewSmallArms(crew, 0, person);

            assertEquals(4, crew.getSmallArms(0));
            assertTrue(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void smallArmsWithoutAptitudeSendsTheSkillOnly() {
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 3, 4, false);
            Crew crew = crewWithEveryAptitude();

            unit.updateCrewSmallArms(crew, 0, person);

            assertEquals(4, crew.getSmallArms(0));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void bestInfantrySkillIsUsed() {
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 2, 6, false);
            giveSkill(person, SkillType.S_SUPPORT_WEAPONS, 6, 3, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewSmallArms(crew, 0, person);

            assertEquals(3, crew.getSmallArms(0));
            assertTrue(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void withSmallArmsOnlyOnlySmallArmsIsUsed() {
            setUseSmallArmsOnly(true);
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 2, 6, false);
            giveSkill(person, SkillType.S_SUPPORT_WEAPONS, 6, 3, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewSmallArms(crew, 0, person);

            assertEquals(6, crew.getSmallArms(0));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void withSmallArmsOnlyAPersonWithoutSmallArmsIsLeftUnset() {
            setUseSmallArmsOnly(true);
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_ARCHERY, 6, 3, true);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewSmallArms(crew, 0, person);

            assertFalse(crew.hasSmallArms(0), "MegaMek should fall back to its default skill");
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void personWithoutAnyInfantrySkillIsLeftUnset() {
            Crew crew = new Crew(CrewType.SINGLE);
            crew.setSmallArms(4, 0);
            crew.setHasNaturalAptitudeSmallArms(true, 0);

            unit.updateCrewSmallArms(crew, 0, mock(Person.class));

            assertFalse(crew.hasSmallArms(0));
            assertEquals(Crew.SMALL_ARMS_UNSET, crew.getSmallArms(0));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void noPersonClearsThePreviousOccupantsSkillAndAptitude() {
            Crew crew = new Crew(CrewType.SINGLE);
            crew.setSmallArms(4, 0);
            crew.setHasNaturalAptitudeSmallArms(true, 0);

            unit.updateCrewSmallArms(crew, 0, null);

            assertFalse(crew.hasSmallArms(0));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void advancedMedicalAddsInjuryModifiers() {
            setUseAdvancedMedical(true);
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 3, 4, false);
            when(person.getInjuryModifiers(false)).thenReturn(2);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewSmallArms(crew, 0, person);

            assertEquals(6, crew.getSmallArms(0));
        }

        @Test
        void injuryModifiersAreIgnoredWithoutAdvancedMedical() {
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 3, 4, false);
            lenient().when(person.getInjuryModifiers(false)).thenReturn(2);
            Crew crew = new Crew(CrewType.SINGLE);

            unit.updateCrewSmallArms(crew, 0, person);

            assertEquals(4, crew.getSmallArms(0));
        }

        @Test
        void skillIsClampedToWhatMegaMekAccepts() {
            Person poorShot = mock(Person.class);
            giveSkill(poorShot, SkillType.S_SMALL_ARMS, 0, 11, false);
            Person deadEye = mock(Person.class);
            giveSkill(deadEye, SkillType.S_SMALL_ARMS, 10, -2, false);
            Crew crew = new Crew(CrewType.TRIPOD);

            unit.updateCrewSmallArms(crew, 0, poorShot);
            unit.updateCrewSmallArms(crew, 1, deadEye);

            assertEquals(Crew.MAX_SKILL, crew.getSmallArms(0));
            assertEquals(0, crew.getSmallArms(1));
        }

        @Test
        void chassisFamiliarityIsNotApplied() {
            Person person = mock(Person.class);
            Skill smallArms = giveSkill(person, SkillType.S_SMALL_ARMS, 3, 4, false);

            unit.updateCrewSmallArms(new Crew(CrewType.SINGLE), 0, person);

            verify(smallArms, never()).getFinalSkillValue(any(), anyInt());
        }

        @Test
        void onlyTheGivenSlotIsUpdated() {
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 3, 4, true);
            Crew crew = new Crew(CrewType.TRIPOD);

            unit.updateCrewSmallArms(crew, 1, person);

            assertEquals(4, crew.getSmallArms(1));
            assertTrue(crew.isHasNaturalAptitudeSmallArms(1));
            assertFalse(crew.hasSmallArms(0));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }
    }

    @Nested
    class InfantryGunnerySkill {
        @Test
        void bestInfantrySkillIsChosen() {
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 2, 6, false);
            giveSkill(person, SkillType.S_SUPPORT_WEAPONS, 6, 3, false);

            assertEquals(SkillType.S_SUPPORT_WEAPONS, unit.getInfantryGunnerySkill(person));
        }

        @Test
        void smallArmsOnlyChoosesSmallArms() {
            setUseSmallArmsOnly(true);
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_SMALL_ARMS, 2, 6, false);
            giveSkill(person, SkillType.S_SUPPORT_WEAPONS, 6, 3, false);

            assertEquals(SkillType.S_SMALL_ARMS, unit.getInfantryGunnerySkill(person));
        }

        @Test
        void personWithNoInfantrySkillFallsBackToSmallArms() {
            assertEquals(SkillType.S_SMALL_ARMS, unit.getInfantryGunnerySkill(mock(Person.class)));
        }

        @Test
        void smallArmsOnlyWithoutSmallArmsFallsBackToSmallArms() {
            setUseSmallArmsOnly(true);
            Person person = mock(Person.class);
            giveSkill(person, SkillType.S_ARCHERY, 6, 3, false);

            assertEquals(SkillType.S_SMALL_ARMS, unit.getInfantryGunnerySkill(person));
        }
    }
}

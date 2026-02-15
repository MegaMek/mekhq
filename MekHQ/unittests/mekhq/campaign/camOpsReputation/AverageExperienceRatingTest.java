/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.camOpsReputation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AverageExperienceRatingTest {

    @Test
    void returnsNoCampaignExperience_whenNoCombatTeams() throws Exception {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getHangar()).thenReturn(mock(Hangar.class));
        when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>());

        assertEquals(7, invokeCalculateAverageExperienceRating(campaign, false));
        assertEquals(7, invokeCalculateAverageExperienceRating(campaign, true));
    }

    @Test
    void returnsNoCampaignExperience_whenAllCombatTeamsReturnNullForce() throws Exception {
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        CombatTeam team = mock(CombatTeam.class);
        when(team.getFormation(campaign)).thenReturn(null);
        when(team.getFormationId()).thenReturn(123);

        when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

        assertEquals(7, invokeCalculateAverageExperienceRating(campaign, false));
    }

    @Test
    void returnsNoCampaignExperience_whenAllForcesAreTraining() throws Exception {
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        Formation trainingFormation = mock(Formation.class, RETURNS_DEEP_STUBS);
        when(trainingFormation.getCombatRoleInMemory().isTraining()).thenReturn(true);

        CombatTeam team = mock(CombatTeam.class);
        when(team.getFormation(campaign)).thenReturn(trainingFormation);

        when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

        assertEquals(7, invokeCalculateAverageExperienceRating(campaign, false));
    }

    @Test
    void returnsNoCampaignExperience_whenUnitsAreUncrewed() throws Exception {
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        Entity entity = mock(Entity.class);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getCommander()).thenReturn(null); // uncrewed

        Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
        when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
        when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unit));

        CombatTeam team = mock(CombatTeam.class);
        when(team.getFormation(campaign)).thenReturn(formation);

        when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

        assertEquals(7, invokeCalculateAverageExperienceRating(campaign, false));
    }

    @Test
    void ignoresJumpships_entirely() throws Exception {
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        Jumpship jumpship = mock(Jumpship.class); // instanceof Jumpship => must be skipped
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(jumpship);

        Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
        when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
        when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unit));

        CombatTeam team = mock(CombatTeam.class);
        when(team.getFormation(campaign)).thenReturn(formation);

        when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

        assertEquals(7, invokeCalculateAverageExperienceRating(campaign, false));
    }

    @Test
    void computesAverage_forNonSmallCraftCommander_andRoundsHalfDown() throws Exception {
        // One unit: piloting=4, gunnery=3 => totalExperience=7
        // unitCount=1 => divisor=2 => rawAverage=3.5 => fractional==0.5 => round DOWN => 3
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        Entity entity = mock(Entity.class);

        SkillModifierData modData = mock(SkillModifierData.class);
        Person commander = mock(Person.class);
        when(commander.getSkillModifierData(true)).thenReturn(modData);

        Skill driving = mock(Skill.class);
        Skill gunnery = mock(Skill.class);
        when(driving.getFinalSkillValue(modData)).thenReturn(4);
        when(gunnery.getFinalSkillValue(modData)).thenReturn(3);

        try (MockedStatic<SkillType> skillType = mockStatic(SkillType.class)) {
            skillType.when(() -> SkillType.getDrivingSkillFor(entity)).thenReturn("Driving");
            skillType.when(() -> SkillType.getGunnerySkillFor(entity)).thenReturn("Gunnery");

            when(commander.getSkill("Driving")).thenReturn(driving);
            when(commander.getSkill("Gunnery")).thenReturn(gunnery);

            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getCommander()).thenReturn(commander);

            Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
            when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
            when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unit));

            CombatTeam team = mock(CombatTeam.class);
            when(team.getFormation(campaign)).thenReturn(formation);

            when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

            assertEquals(3, invokeCalculateAverageExperienceRating(campaign, false));
        }
    }

    @Test
    void computesAverage_forMultipleNonSmallCraftUnits_andRoundsUpWhenFractionGreaterThanHalf() throws Exception {
        // Unit A: piloting=3, gunnery=4 => 7
        // Unit B: piloting=3, gunnery=5 => 8
        // totalExperience=15, units=2 => divisor=4 => rawAverage=3.75 => fractional>0.5 => ceil => 4
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        Entity entityA = mock(Entity.class);
        Entity entityB = mock(Entity.class);

        SkillModifierData modData = mock(SkillModifierData.class);
        Person commanderA = mock(Person.class);
        Person commanderB = mock(Person.class);
        when(commanderA.getSkillModifierData(true)).thenReturn(modData);
        when(commanderB.getSkillModifierData(true)).thenReturn(modData);

        Skill aDriving = mock(Skill.class);
        Skill aGunnery = mock(Skill.class);
        when(aDriving.getFinalSkillValue(modData)).thenReturn(3);
        when(aGunnery.getFinalSkillValue(modData)).thenReturn(4);

        Skill bDriving = mock(Skill.class);
        Skill bGunnery = mock(Skill.class);
        when(bDriving.getFinalSkillValue(modData)).thenReturn(3);
        when(bGunnery.getFinalSkillValue(modData)).thenReturn(5);

        try (MockedStatic<SkillType> skillType = mockStatic(SkillType.class)) {
            skillType.when(() -> SkillType.getDrivingSkillFor(entityA)).thenReturn("Driving");
            skillType.when(() -> SkillType.getGunnerySkillFor(entityA)).thenReturn("Gunnery");
            skillType.when(() -> SkillType.getDrivingSkillFor(entityB)).thenReturn("Driving");
            skillType.when(() -> SkillType.getGunnerySkillFor(entityB)).thenReturn("Gunnery");

            when(commanderA.getSkill("Driving")).thenReturn(aDriving);
            when(commanderA.getSkill("Gunnery")).thenReturn(aGunnery);
            when(commanderB.getSkill("Driving")).thenReturn(bDriving);
            when(commanderB.getSkill("Gunnery")).thenReturn(bGunnery);

            Unit unitA = mock(Unit.class);
            when(unitA.getEntity()).thenReturn(entityA);
            when(unitA.getCommander()).thenReturn(commanderA);

            Unit unitB = mock(Unit.class);
            when(unitB.getEntity()).thenReturn(entityB);
            when(unitB.getCommander()).thenReturn(commanderB);

            Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
            when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
            when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unitA, unitB));

            CombatTeam team = mock(CombatTeam.class);
            when(team.getFormation(campaign)).thenReturn(formation);

            when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

            assertEquals(4, invokeCalculateAverageExperienceRating(campaign, false));
        }
    }

    @Test
    void missingSkills_fallBackToBaseTargetPlusOne() throws Exception {
        // If the person lacks the skill, code uses SkillType.getType(skillName).getTarget() + 1
        // Set target=5 => returns 6 for driving and 6 for gunnery => total=12 => divisor=2 => avg=6
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        Entity entity = mock(Entity.class);

        SkillModifierData modData = mock(SkillModifierData.class);
        Person commander = mock(Person.class);
        when(commander.getSkillModifierData(true)).thenReturn(modData);
        when(commander.getSkill("Driving")).thenReturn(null);
        when(commander.getSkill("Gunnery")).thenReturn(null);

        SkillType drivingType = mock(SkillType.class);
        SkillType gunneryType = mock(SkillType.class);
        when(drivingType.getTarget()).thenReturn(5);
        when(gunneryType.getTarget()).thenReturn(5);

        try (MockedStatic<SkillType> skillType = mockStatic(SkillType.class)) {
            skillType.when(() -> SkillType.getDrivingSkillFor(entity)).thenReturn("Driving");
            skillType.when(() -> SkillType.getGunnerySkillFor(entity)).thenReturn("Gunnery");
            skillType.when(() -> SkillType.getType("Driving")).thenReturn(drivingType);
            skillType.when(() -> SkillType.getType("Gunnery")).thenReturn(gunneryType);

            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getCommander()).thenReturn(commander);

            Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
            when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
            when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unit));

            CombatTeam team = mock(CombatTeam.class);
            when(team.getFormation(campaign)).thenReturn(formation);

            when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

            assertEquals(6, invokeCalculateAverageExperienceRating(campaign, false));
        }
    }

    @Test
    void smallCraft_averagesDriversAndGunners_andRoundsEachRoleAverage() throws Exception {
        // SmallCraft branch averages each role separately (with Math.round), then adds (pilotAvg + gunnerAvg).
        // Drivers (2): 4 and 6 => avg=5.0 => round => 5
        // Gunners (2): 3 and 3 => avg=3.0 => round => 3
        // totalExperience=8 => divisor=2 => avg=4
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        SmallCraft smallCraft = mock(SmallCraft.class);

        SkillModifierData modData = mock(SkillModifierData.class);

        Person driver1 = mock(Person.class);
        Person driver2 = mock(Person.class);
        when(driver1.getSkillModifierData(true)).thenReturn(modData);
        when(driver2.getSkillModifierData(true)).thenReturn(modData);

        Person gunner1 = mock(Person.class);
        Person gunner2 = mock(Person.class);
        when(gunner1.getSkillModifierData(true)).thenReturn(modData);
        when(gunner2.getSkillModifierData(true)).thenReturn(modData);

        Skill driving1 = mock(Skill.class);
        Skill driving2 = mock(Skill.class);
        when(driving1.getFinalSkillValue(modData)).thenReturn(4);
        when(driving2.getFinalSkillValue(modData)).thenReturn(6);

        Skill gunnery1 = mock(Skill.class);
        Skill gunnery2 = mock(Skill.class);
        when(gunnery1.getFinalSkillValue(modData)).thenReturn(3);
        when(gunnery2.getFinalSkillValue(modData)).thenReturn(3);

        try (MockedStatic<SkillType> skillType = mockStatic(SkillType.class)) {
            skillType.when(() -> SkillType.getDrivingSkillFor(smallCraft)).thenReturn("Driving");
            skillType.when(() -> SkillType.getGunnerySkillFor(smallCraft)).thenReturn("Gunnery");

            when(driver1.getSkill("Driving")).thenReturn(driving1);
            when(driver2.getSkill("Driving")).thenReturn(driving2);
            when(gunner1.getSkill("Gunnery")).thenReturn(gunnery1);
            when(gunner2.getSkill("Gunnery")).thenReturn(gunnery2);

            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(smallCraft);
            when(unit.getDrivers()).thenReturn(List.of(driver1, driver2));
            when(unit.getGunners()).thenReturn(Set.of(gunner1, gunner2));

            Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
            when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
            when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unit));

            CombatTeam team = mock(CombatTeam.class);
            when(team.getFormation(campaign)).thenReturn(formation);

            when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

            assertEquals(4, invokeCalculateAverageExperienceRating(campaign, false));
        }
    }

    @Test
    void smallCraft_withSingleDriverAndSingleGunner_isTreatedAsNoCrew_andReturnsNoCampaignExperience()
          throws Exception {
        // In the SmallCraft branch, hasAtLeastOneCrew is only set true when a role has > 1 person.
        // So (1 driver, 1 gunner) leaves hasAtLeastOneCrew false and should return NO_CAMPAIGN_EXPERIENCE.
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        SmallCraft smallCraft = mock(SmallCraft.class);

        SkillModifierData modData = mock(SkillModifierData.class);
        Person driver = mock(Person.class);
        Person gunner = mock(Person.class);
        when(driver.getSkillModifierData(true)).thenReturn(modData);
        when(gunner.getSkillModifierData(true)).thenReturn(modData);

        Skill driving = mock(Skill.class);
        Skill gunnery = mock(Skill.class);
        when(driving.getFinalSkillValue(modData)).thenReturn(4);
        when(gunnery.getFinalSkillValue(modData)).thenReturn(3);

        try (MockedStatic<SkillType> skillType = mockStatic(SkillType.class)) {
            skillType.when(() -> SkillType.getDrivingSkillFor(smallCraft)).thenReturn("Driving");
            skillType.when(() -> SkillType.getGunnerySkillFor(smallCraft)).thenReturn("Gunnery");

            when(driver.getSkill("Driving")).thenReturn(driving);
            when(gunner.getSkill("Gunnery")).thenReturn(gunnery);

            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(smallCraft);
            when(unit.getDrivers()).thenReturn(List.of(driver));
            when(unit.getGunners()).thenReturn(Set.of(gunner));

            Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
            when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
            when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unit));

            CombatTeam team = mock(CombatTeam.class);
            when(team.getFormation(campaign)).thenReturn(formation);

            when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

            assertEquals(7, invokeCalculateAverageExperienceRating(campaign, false));
        }
    }

    @Test
    void logFlag_doesNotChangeComputedResult() throws Exception {
        Campaign campaign = mock(Campaign.class);
        Hangar hangar = mock(Hangar.class);
        when(campaign.getHangar()).thenReturn(hangar);

        Entity entity = mock(Entity.class);

        SkillModifierData modData = mock(SkillModifierData.class);
        Person commander = mock(Person.class);
        when(commander.getSkillModifierData(true)).thenReturn(modData);

        Skill driving = mock(Skill.class);
        Skill gunnery = mock(Skill.class);
        when(driving.getFinalSkillValue(modData)).thenReturn(4);
        when(gunnery.getFinalSkillValue(modData)).thenReturn(3);

        try (MockedStatic<SkillType> skillType = mockStatic(SkillType.class)) {
            skillType.when(() -> SkillType.getDrivingSkillFor(entity)).thenReturn("Driving");
            skillType.when(() -> SkillType.getGunnerySkillFor(entity)).thenReturn("Gunnery");
            when(commander.getSkill("Driving")).thenReturn(driving);
            when(commander.getSkill("Gunnery")).thenReturn(gunnery);

            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getCommander()).thenReturn(commander);

            Formation formation = mock(Formation.class, RETURNS_DEEP_STUBS);
            when(formation.getCombatRoleInMemory().isTraining()).thenReturn(false);
            when(formation.getAllUnitsAsUnits(hangar, true)).thenReturn(List.of(unit));

            CombatTeam team = mock(CombatTeam.class);
            when(team.getFormation(campaign)).thenReturn(formation);

            when(campaign.getCombatTeamsAsList()).thenReturn(new ArrayList<>(List.of(team)));

            assertEquals(invokeCalculateAverageExperienceRating(campaign, false),
                  invokeCalculateAverageExperienceRating(campaign, true));
        }
    }

    private static int invokeCalculateAverageExperienceRating(Campaign campaign, boolean log) throws Exception {
        Method calculateAverageExperienceRating = AverageExperienceRating.class.getDeclaredMethod(
              "calculateAverageExperienceRating",
              Campaign.class,
              boolean.class
        );
        calculateAverageExperienceRating.setAccessible(true);
        return (int) calculateAverageExperienceRating.invoke(null, campaign, log);
    }
}

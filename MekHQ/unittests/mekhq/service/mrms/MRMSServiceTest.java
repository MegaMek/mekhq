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
package mekhq.service.mrms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getEntityForUnitTesting;

import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * JUnit Tests for {@link MRMSService}
 */
public class MRMSServiceTest {
    static MMLogger LOGGER = MMLogger.create(MRMSServiceTest.class);

    static int DEFAULT_TARGET_NUMBER = 6;

    static Faction mockFaction;
    static List<WorkTime> timeSpent;

    Campaign mockCampaign;
    CampaignOptions mockCampaignOptions;
    Warehouse warehouse;
    Quartermaster mockQuartermaster;
    MRMSConfiguredOptions configuredOptions;

    int targetRoll = DEFAULT_TARGET_NUMBER;
    IPartWork lastPartWork;

    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        SkillType.initializeTypes();

        mockFaction = Mockito.mock(Faction.class);
        when(mockFaction.getShortName()).thenReturn("Faction");

    }

    @BeforeEach
    public void beforeEach() {
        timeSpent = new ArrayList<>();

        targetRoll = DEFAULT_TARGET_NUMBER;
        lastPartWork = null;

        TargetRoll mockBaseTargetRoll = mock(TargetRoll.class);
        when(mockBaseTargetRoll.getValue()).thenReturn(targetRoll);

        mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.getMRMSOptions()).thenReturn(new ArrayList<>());

        warehouse = new Warehouse();

        mockQuartermaster = mock(Quartermaster.class);

        mockCampaign = mock(Campaign.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        when(mockCampaign.getQuartermaster()).thenReturn(mockQuartermaster);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockCampaign.fixPart(any(IPartWork.class), any(Person.class))).thenReturn("Part Fixed");

        //Part p = mock(Part.class);
        when(mockCampaign.getTargetFor(any(IPartWork.class), any(Person.class))).thenReturn(mockBaseTargetRoll);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            if (part.equals(lastPartWork) && part.getMode() == lastPartWork.getMode()) {
                return mockBaseTargetRoll;
            } else {
                if (lastPartWork == null) {
                    targetRoll = DEFAULT_TARGET_NUMBER;
                } else {
                    if (part.getMode() == WorkTime.NORMAL) {
                        targetRoll = DEFAULT_TARGET_NUMBER;
                    } else if (part.getMode().isRushed) {
                        targetRoll++;
                    } else {
                        targetRoll--;

                    }
                }
                lastPartWork = part.clone();
                when(mockBaseTargetRoll.getValue()).thenReturn(targetRoll);
            }
            return mockBaseTargetRoll;
        }).when(mockCampaign).getTargetFor(any(Part.class), any(Person.class));
    }


    @Test
    public void testMRMS() {
        int skillMin = SkillLevel.ULTRA_GREEN.getExperienceLevel();
        int skillMax = SkillLevel.HEROIC.getExperienceLevel();
        int targetNumberPreferred = 6;
        int targetNumberMax = 6;
        int dailyTimeMin = 0;

        Entity entity = getUrbanMek();
        Unit unit = new Unit(entity, mockCampaign);
        addMRMSOption(PartRepairType.ARMOUR, skillMin, skillMax, targetNumberPreferred, targetNumberMax, dailyTimeMin);

        when(mockCampaignOptions.isMRMSUseRepair()).thenReturn(true);

        Person mockTech = mock(Person.class);
        when(mockCampaign.getTechs(anyBoolean())).thenReturn(List.of(mockTech));
        when(mockTech.canTech(unit.getEntity())).thenReturn(true);
        when(mockTech.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.VETERAN);
        when(mockTech.getSkillForWorkingOn(any(IPartWork.class))).thenReturn(new Skill(SkillType.S_TECH_MEK, 7, 0));
        when(mockTech.getMinutesLeft()).thenReturn(480);
        when(mockTech.getATOWAttributes()).thenReturn(new Attributes());

        configuredOptions = new MRMSConfiguredOptions(mockCampaign);
        unit.setTech(mockTech);

        unit.getEntity().setArmor(1, Mek.LOC_HEAD);
        unit.initializeParts(true);
        unit.getEntity().setArmor(0, Mek.LOC_HEAD);
        unit.getParts().stream().filter(p -> p instanceof Armor).map(p -> (Armor) p).forEach(this::breakArmor);

        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(6);
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }


        verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
    }

    private static Entity getUrbanMek() {
        String unitName = "UrbanMech UM-R69";
        Entity entity = getEntityForUnitTesting(unitName, false);
        assertNotNull(entity, "Entity not found for " + unitName);
        return entity;
    }

    @Nested
    public class testMRMSUnitsSkillLevels {
        Unit unit;

        // Values not tested in this test:
        static final int targetNumberPreferred = 6;
        static final int targetNumberMax = 6;
        static final int dailyTimeMin = 0;

        @BeforeEach
        public void beforeEach() {
            when(mockCampaignOptions.isMRMSUseRepair()).thenReturn(true);

            Entity entity = getUrbanMek();
            assert entity != null;
            unit = new Unit(entity, mockCampaign);
            unit.initializeParts(true);
        }

        @Test
        public void testControlMRMSUnitsSkills() {
            // Arrange
            int skillMin = SkillLevel.ULTRA_GREEN.getExperienceLevel();
            int skillMax = SkillLevel.LEGENDARY.getExperienceLevel();

            arrangeTestMRMSUnits(skillMin, skillMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);

            // Assert
            verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
        }

        @Test
        public void testMRMSUnitsBelowPreferredSkill() {
            // Arrange
            int skillMin = SkillLevel.ELITE.getExperienceLevel();
            int skillMax = SkillLevel.LEGENDARY.getExperienceLevel();

            arrangeTestMRMSUnits(skillMin, skillMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);

            // Assert
            verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
        }

        @Test
        public void testMRMSUnitsAboveMaxSkill() {
            // Arrange
            int skillMin = SkillLevel.ULTRA_GREEN.getExperienceLevel();
            int skillMax = SkillLevel.GREEN.getExperienceLevel();

            arrangeTestMRMSUnits(skillMin, skillMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);

            // Assert
            verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
        }

        private void arrangeTestMRMSUnits(int skillMin, int skillMax) {
            addMRMSOption(PartRepairType.ARMOUR, skillMin, skillMax, targetNumberPreferred,
                  targetNumberMax, dailyTimeMin);
            configuredOptions = new MRMSConfiguredOptions(mockCampaign);

            addMockTech(SkillType.S_TECH_MEK, SkillLevel.VETERAN);

            unit.getParts()
                  .stream()
                  .filter(p -> p instanceof Armor)
                  .map(p -> (Armor) p)
                  .forEach(MRMSServiceTest.this::breakArmor);
        }
    }


    @Nested
    public class testMRMSUnitsTargetNumbers {
        int TN_IS_ABOVE = DEFAULT_TARGET_NUMBER - 1;
        int TN_IS_BELOW = DEFAULT_TARGET_NUMBER + 1;

        int TN_IS_WAY_ABOVE = DEFAULT_TARGET_NUMBER - 4;
        int TN_IS_WAY_BELOW = DEFAULT_TARGET_NUMBER + 4;

        Unit unit;

        // Values not tested in this test:
        static final int skillMin = SkillLevel.ULTRA_GREEN.getExperienceLevel();
        static final int skillMax = SkillLevel.ELITE.getExperienceLevel();
        static final int dailyTimeMin = 0;

        @BeforeEach
        public void beforeEach() {
            when(mockCampaignOptions.isMRMSUseRepair()).thenReturn(true);

            Entity entity = getUrbanMek();
            unit = new Unit(entity, mockCampaign);
            unit.initializeParts(true);
        }

        @Test
        public void testControlMRMSUnitsTargetNumbers() {
            // Arrange
            int targetNumberPreferred = DEFAULT_TARGET_NUMBER;
            int targetNumberMax = DEFAULT_TARGET_NUMBER;

            when(mockCampaignOptions.isMRMSUseExtraTime()).thenReturn(true);
            when(mockCampaignOptions.isMRMSUseRushJob()).thenReturn(true);

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);

            // Assert
            verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
            assertEquals(11, timeSpent.size());
            for (int i = 0; i < timeSpent.size(); i++) {
                assertEquals(WorkTime.NORMAL, timeSpent.get(i), "i=" + i);
            }
        }

        /**
         * When "Use Extra Time" and "Use Rush Job" options are deactivated, MRMS will succeed unless the part's TN is
         * higher than the "Max TN" setting ("Preferred TN" not considered).
         */
        @Nested
        public class TestNoExtraTimeNorUseRush {

            @BeforeEach
            public void beforeEach() {
                when(mockCampaignOptions.isMRMSUseExtraTime()).thenReturn(false);
                when(mockCampaignOptions.isMRMSUseRushJob()).thenReturn(false);
            }

            /**
             * Unit isn't repaired if the TN is above the "max TN" setting
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNAbovePreferredAboveMax() {
                doMRMSUnitsTargetNumbersWhereTNAbovePreferredAboveMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit is repaired if the TN is equal or lower than the "max TN" setting
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNAbovePreferredBelowMax() {
                doMRMSUnitsTargetNumbersWhereTNAbovePreferredBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.NORMAL, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit isn't repaired if the TN is above the "max TN" setting
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNBelowPreferredAboveMax() {
                doMRMSUnitsTargetNumbersWhereTNBelowPreferredAboveMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit is repaired if the TN is equal or lower than the "max TN" setting
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNBelowPreferredBelowMax() {
                doMRMSUnitsTargetNumbersWhereTNBelowPreferredBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.NORMAL, timeSpent.get(i), "i=" + i);
                }
            }
        }

        @Nested
        public class TestExtraTimeAndUseRush {

            @BeforeEach
            public void beforeEach() {
                when(mockCampaignOptions.isMRMSUseExtraTime()).thenReturn(true);
                when(mockCampaignOptions.isMRMSUseRushJob()).thenReturn(true);
            }

            /**
             * Unit is repaired even if the TN is above the "max TN" setting when it is able to reduce the TN to equal
             * or lower than the "max TN" setting via extra time. The MRMS will try to get the adjusted TN as close as
             * possible to the "Preferred TN" setting.
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNAbovePreferredAboveMax() {
                doMRMSUnitsTargetNumbersWhereTNAbovePreferredAboveMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.EXTRA_2, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit is repaired even if the TN is above the "Max TN" setting when it is able to reduce the TN to equal
             * or lower than the "max TN" setting via extra time. The MRMS will try to get the adjusted TN as close as
             * possible to the "Preferred TN" setting, using extra time if it currently above it.
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNAbovePreferredBelowMax() {
                doMRMSUnitsTargetNumbersWhereTNAbovePreferredBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.EXTRA_2, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit isn't repaired if the TN is below the "Preferred TN" setting and above the "Max TN" setting - the
             * "Preferred TN" setting must be equal or lower than the "Max TN" setting.
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNBelowPreferredAboveMax() {
                doMRMSUnitsTargetNumbersWhereTNBelowPreferredAboveMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit is always repaired if the TN is below the "Max TN" setting. The MRMS will try to get the adjusted TN
             * as close as possible to the "Preferred TN" setting, using rush job if it currently below it.
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNBelowPreferredBelowMax() {
                doMRMSUnitsTargetNumbersWhereTNBelowPreferredBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.RUSH_2, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit isn't repaired if the TN is above the "Max TN" setting, and we can't reach it even with extra time
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNWayAbovePreferredWayAboveMax() {
                doMRMSUnitsTargetNumbersWhereTNWayAbovePreferredWayAboveMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit is always repaired if the TN is below the "Max TN" setting. The MRMS will try to get the adjusted TN
             * as close as possible to the "Preferred TN" setting, using extra time if it currently above it.
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNWayAbovePreferredWayBelowMax() {
                doMRMSUnitsTargetNumbersWhereTNWayAbovePreferredWayBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.EXTRA_4, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit isn't repaired if the TN is below the "Preferred TN" setting and above the "Max TN" setting - the
             * "Preferred TN" setting must be equal or lower than the "Max TN" setting.
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNWayBelowPreferredWayAboveMax() {
                doMRMSUnitsTargetNumbersWhereTNWayBelowPreferredWayAboveMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit is always repaired if the TN is below the "Max TN" setting. The MRMS will try to get the adjusted TN
             * as close as possible to the "Preferred TN" setting, using rush job if it currently below it.
             */
            @Test
            public void testMRMSUnitsTargetNumbersIfTNWayBelowPreferredWayBelowMax() {
                doMRMSUnitsTargetNumbersWhereTNWayBelowPreferredWayBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.RUSH_8, timeSpent.get(i), "i=" + i);
                }
            }
        }

        private void arrangeTestMRMSUnits(int targetNumberPreferred, int targetNumberMax) {
            addMRMSOption(PartRepairType.ARMOUR, skillMin, skillMax, targetNumberPreferred, targetNumberMax,
                  dailyTimeMin);
            configuredOptions = new MRMSConfiguredOptions(mockCampaign);

            addMockTech(SkillType.S_TECH_MEK, SkillLevel.VETERAN);

            unit.getParts()
                  .stream()
                  .filter(p -> p instanceof Armor)
                  .map(p -> (Armor) p)
                  .forEach(MRMSServiceTest.this::breakArmor);
        }

        private void doMRMSUnitsTargetNumbersWhereTNAbovePreferredAboveMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_ABOVE;
            int targetNumberMax = TN_IS_ABOVE;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsTargetNumbersWhereTNAbovePreferredBelowMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_ABOVE;
            int targetNumberMax = TN_IS_BELOW;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsTargetNumbersWhereTNBelowPreferredAboveMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_BELOW;
            int targetNumberMax = TN_IS_ABOVE;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        public void doMRMSUnitsTargetNumbersWhereTNBelowPreferredBelowMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_BELOW;
            int targetNumberMax = TN_IS_BELOW;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsTargetNumbersWhereTNWayAbovePreferredWayAboveMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_WAY_ABOVE;
            int targetNumberMax = TN_IS_WAY_ABOVE;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsTargetNumbersWhereTNWayAbovePreferredWayBelowMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_WAY_ABOVE;
            int targetNumberMax = TN_IS_WAY_BELOW;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsTargetNumbersWhereTNWayBelowPreferredWayAboveMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_WAY_BELOW;
            int targetNumberMax = TN_IS_WAY_ABOVE;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        public void doMRMSUnitsTargetNumbersWhereTNWayBelowPreferredWayBelowMax() {
            // Arrange
            int targetNumberPreferred = TN_IS_WAY_BELOW;
            int targetNumberMax = TN_IS_WAY_BELOW;

            arrangeTestMRMSUnits(targetNumberPreferred, targetNumberMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }
    }

    private void breakArmor(Armor armor) {
        int armorAmount = armor.getAmount();
        doAnswer(inv -> {
            armor.setAmountNeeded(0);
            armor.setAmount(armorAmount);
            //when(((Person) inv.getArgument(1)).getMinutesLeft()).thenReturn()
            timeSpent.add(((Armor) inv.getArgument(0)).getMode());
            return null;
        }).when(mockCampaign).fixPart(argThat(new IPartWorkMatch(armor)), any(Person.class));
        warehouse.addPart(armor.clone(), true);
        armor.setAmountNeeded(armorAmount);
        armor.setAmount(0);
    }

    private static class IPartWorkMatch implements ArgumentMatcher<IPartWork> {
        Part part;

        IPartWorkMatch(Part part) {
            this.part = part;
        }

        @Override
        public boolean matches(IPartWork iPartWork) {
            return iPartWork.equals(part);
        }
    }

    private void addMockTech(String skillType, SkillLevel skillLevel) {
        Person mockTech = mock(Person.class);
        when(mockCampaign.getTechs(anyBoolean())).thenReturn(List.of(mockTech));
        when(mockTech.canTech(any(Entity.class))).thenReturn(true);
        when(mockTech.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(skillLevel);
        when(mockTech.getSkillForWorkingOn(any(IPartWork.class))).thenReturn(new Skill(skillType,
              skillLevel.getExperienceLevel(),
              0));
        when(mockTech.getMinutesLeft()).thenReturn(480);
        when(mockTech.getATOWAttributes()).thenReturn(new Attributes());

    }

    private void addMRMSOption(PartRepairType partRepairType, int skillMin, int skillMax, int targetNumberPreferred,
          int targetNumberMax, int dailyTimeMin) {
        List<MRMSOption> mrmsOptions = mockCampaignOptions.getMRMSOptions();
        MRMSOption mrm = new MRMSOption(partRepairType, true, skillMin, skillMax, targetNumberPreferred,
              targetNumberMax, dailyTimeMin);
        mrmsOptions.add(mrm);
        when(mockCampaignOptions.getMRMSOptions()).thenReturn(mrmsOptions);
    }

}

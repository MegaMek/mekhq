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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mek;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.TargetRoll;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Systems;
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
    static MMLogger logger = MMLogger.create(MRMSServiceTest.class);

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
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            logger.error("", ex);
        }

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
        when(mockCampaignOptions.getMRMSOptions()).thenReturn(new ArrayList<MRMSOption>());

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
        int bthMin = 6;
        int bthMax = 6;
        int dailyTimeMin = 0;

        Unit unit = new Unit(createEntity("UrbanMech UM-R69"), mockCampaign);

        addMRMSOption(PartRepairType.ARMOUR, skillMin, skillMax, bthMin, bthMax, dailyTimeMin);

        when(mockCampaignOptions.isMRMSUseRepair()).thenReturn(true);

        Person mockTech = mock(Person.class);
        when(mockCampaign.getTechs(anyBoolean())).thenReturn(List.of(mockTech));
        when(mockTech.canTech(unit.getEntity())).thenReturn(true);
        when(mockTech.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.VETERAN);
        when(mockTech.getSkillForWorkingOn(any(IPartWork.class))).thenReturn(new Skill(SkillType.S_TECH_MEK, 7, 0));
        when(mockTech.getMinutesLeft()).thenReturn(480);

        configuredOptions = new MRMSConfiguredOptions(mockCampaign);
        unit.setTech(mockTech);

        unit.getEntity().setArmor(1, Mek.LOC_HEAD);
        unit.initializeParts(true);
        unit.getEntity().setArmor(0, Mek.LOC_HEAD);
        unit.getParts().stream().filter(p -> p instanceof Armor).map(p -> (Armor) p).forEach(armor -> {
            breakArmor(armor);

        });

        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(6);
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }


        verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
    }

    @Nested
    public class testMRMSUnitsSkillLevels {
        Unit unit;

        // Values not tested in this test:
        static final int bthMin = 6;
        static final int bthMax = 6;
        static final int dailyTimeMin = 0;

        @BeforeEach
        public void beforeEach() {
            when(mockCampaignOptions.isMRMSUseRepair()).thenReturn(true);

            unit = new Unit(createEntity("UrbanMech UM-R69"), mockCampaign);
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
        public void testMRMSUnitsBelowMinSkill() {
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
            addMRMSOption(PartRepairType.ARMOUR, skillMin, skillMax, bthMin, bthMax, dailyTimeMin);
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
    public class testMRMSUnitsBTH {
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

            unit = new Unit(createEntity("UrbanMech UM-R69"), mockCampaign);
            unit.initializeParts(true);
        }

        @Test
        public void testControlMRMSUnitsBTH() {
            // Arrange
            int bthMin = DEFAULT_TARGET_NUMBER;
            int bthMax = DEFAULT_TARGET_NUMBER;

            when(mockCampaignOptions.isMRMSUseExtraTime()).thenReturn(true);
            when(mockCampaignOptions.isMRMSUseRushJob()).thenReturn(true);

            arrangeTestMRMSUnits(bthMin, bthMax);

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
         * When "Use Extra Time" and "Use Rush Job" are deactivated, MRMS will succeed if the TN is above the "Min TN"
         * and will not consider the "Max TN".
         */
        @Nested
        public class TestNoExtraTimeNorUseRush {
            int TN_IS_ABOVE = DEFAULT_TARGET_NUMBER - 1;
            int TN_IS_BELOW = DEFAULT_TARGET_NUMBER + 1;

            @BeforeEach
            public void beforeEach() {
                when(mockCampaignOptions.isMRMSUseExtraTime()).thenReturn(false);
                when(mockCampaignOptions.isMRMSUseRushJob()).thenReturn(false);
            }

            /**
             * Unit isn't repaired if the TN is above the minimum `bthMin` (`bthMax` not considered)
             */
            @Test
            public void testMRMSUnitsBTHifTNAboveMinAboveMax() {
                doMRMSUnitsBTHWhereTNAboveMinAboveMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit isn't repaired if the TN is above the minimum `bthMin` (`bthMax` not considered)
             */
            @Test
            public void testMRMSUnitsBTHifTNAboveMinBelowMax() {
                doMRMSUnitsBTHWhereTNAboveMinBelowMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit is repaired if the TN is below the minimum `bthMin` and above the maximum `bthMax`
             */
            @Test
            public void testMRMSUnitsBTHifTNBelowMinAboveMax() {
                doMRMSUnitsBTHWhereTNBelowMinAboveMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.NORMAL, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit is repaired if the TN is below the minimum `bthMin` and below the maximum `bthMax`
             */
            @Test
            public void testMRMSUnitsBTHifTNBelowMinBelowMax() {
                doMRMSUnitsBTHWhereTNBelowMinBelowMax();

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
             * Unit is repaired if the TN is above the minimum `bthMin` but it is able to reduce the TN to the minimum
             * via extra time. (`bthMax` not considered)
             */
            @Test
            public void testMRMSUnitsBTHifTNAboveMinAboveMax() {
                doMRMSUnitsBTHWhereTNAboveMinAboveMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.EXTRA_2, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit is repaired if the TN is above the minimum `bthMin` but it is able to reduce the TN to the minimum
             * via extra time. (`bthMax` not considered)
             */
            @Test
            public void testMRMSUnitsBTHifTNAboveMinBelowMax() {
                doMRMSUnitsBTHWhereTNAboveMinBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.EXTRA_2, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit is repaired if the TN is below the minimum `bthMin` and above the maximum `bthMax`
             */
            @Test
            public void testMRMSUnitsBTHifTNBelowMinAboveMax() {
                doMRMSUnitsBTHWhereTNBelowMinAboveMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.NORMAL, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit is repaired if the TN is below the minimum `bthMin` and the TN will be increased to reach the
             * maximum `bthMax` via rush time.
             */
            @Test
            public void testMRMSUnitsBTHifTNBelowMinBelowMax() {
                doMRMSUnitsBTHWhereTNBelowMinBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.RUSH_2, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit isn't repaired if the TN is above the minimum `bthMin` and we can't reach it with "Use Extra Time"
             * (`bthMax` not considered)
             */
            @Test
            public void testMRMSUnitsBTHifTNWayAboveMinWayAboveMax() {
                doMRMSUnitsBTHWhereTNWayAboveMinWayAboveMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit isn't repaired if the TN is above the minimum `bthMin` and we can't reach it with "Use Extra Time"
             * (`bthMax` not considered)
             */
            @Test
            public void testMRMSUnitsBTHifTNWayAboveMinWayBelowMax() {
                doMRMSUnitsBTHWhereTNWayAboveMinWayBelowMax();

                // Assert
                verify(mockCampaign, times(0)).fixPart(any(Part.class), any(Person.class));
                assertEquals(0, timeSpent.size());
            }

            /**
             * Unit is repaired if the TN is below the minimum `bthMin` and above the maximum `bthMax`.
             */
            @Test
            public void testMRMSUnitsBTHifTNWayBelowMinWayAboveMax() {
                doMRMSUnitsBTHWhereTNWayBelowMinWayAboveMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.NORMAL, timeSpent.get(i), "i=" + i);
                }
            }

            /**
             * Unit is repaired if the TN is below the minimum `bthMin` and above the maximum `bthMax`. Rush time is
             * used to increase the TN as close as possible, but it will still attempt to fix the part even if it's not
             * reached.
             */
            @Test
            public void testMRMSUnitsBTHifTNWayBelowMinWayBelowMax() {
                doMRMSUnitsBTHWhereTNWayBelowMinWayBelowMax();

                // Assert
                verify(mockCampaign, times(11)).fixPart(any(Part.class), any(Person.class));
                assertEquals(11, timeSpent.size());
                for (int i = 0; i < timeSpent.size(); i++) {
                    assertEquals(WorkTime.RUSH_8, timeSpent.get(i), "i=" + i);
                }
            }
        }

        private void arrangeTestMRMSUnits(int bthMin, int bthMax) {
            addMRMSOption(PartRepairType.ARMOUR, skillMin, skillMax, bthMin, bthMax, dailyTimeMin);
            configuredOptions = new MRMSConfiguredOptions(mockCampaign);

            addMockTech(SkillType.S_TECH_MEK, SkillLevel.VETERAN);

            unit.getParts()
                  .stream()
                  .filter(p -> p instanceof Armor)
                  .map(p -> (Armor) p)
                  .forEach(MRMSServiceTest.this::breakArmor);
        }

        private void doMRMSUnitsBTHWhereTNAboveMinAboveMax() {
            // Arrange
            int bthMin = TN_IS_ABOVE;
            int bthMax = TN_IS_ABOVE;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsBTHWhereTNAboveMinBelowMax() {
            // Arrange
            int bthMin = TN_IS_ABOVE;
            int bthMax = TN_IS_BELOW;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsBTHWhereTNBelowMinAboveMax() {
            // Arrange
            int bthMin = TN_IS_BELOW;
            int bthMax = TN_IS_ABOVE;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        public void doMRMSUnitsBTHWhereTNBelowMinBelowMax() {
            // Arrange
            int bthMin = TN_IS_BELOW;
            int bthMax = TN_IS_BELOW;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsBTHWhereTNWayAboveMinWayAboveMax() {
            // Arrange
            int bthMin = TN_IS_WAY_ABOVE;
            int bthMax = TN_IS_WAY_ABOVE;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsBTHWhereTNWayAboveMinWayBelowMax() {
            // Arrange
            int bthMin = TN_IS_WAY_ABOVE;
            int bthMax = TN_IS_WAY_BELOW;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        private void doMRMSUnitsBTHWhereTNWayBelowMinWayAboveMax() {
            // Arrange
            int bthMin = TN_IS_WAY_BELOW;
            int bthMax = TN_IS_WAY_ABOVE;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }

        public void doMRMSUnitsBTHWhereTNWayBelowMinWayBelowMax() {
            // Arrange
            int bthMin = TN_IS_WAY_BELOW;
            int bthMax = TN_IS_WAY_BELOW;

            arrangeTestMRMSUnits(bthMin, bthMax);

            // Act
            MRMSService.mrmsUnits(mockCampaign, List.of(unit), configuredOptions);
        }
    }

    private void breakPart(Part part) {
        if (part instanceof Armor armor) {
            breakArmor(armor);
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

    private Person addMockTech(String skillType, SkillLevel skillLevel) {
        Person mockTech = mock(Person.class);
        when(mockCampaign.getTechs(anyBoolean())).thenReturn(List.of(mockTech));
        when(mockTech.canTech(any(Entity.class))).thenReturn(true);
        when(mockTech.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(skillLevel);
        when(mockTech.getSkillForWorkingOn(any(IPartWork.class))).thenReturn(new Skill(skillType,
              skillLevel.getExperienceLevel(),
              0));
        when(mockTech.getMinutesLeft()).thenReturn(480);

        return mockTech;
    }

    private void addMRMSOption(PartRepairType partRepairType, int skillMin, int skillMax, int bthMin, int bthMax,
          int dailyTimeMin) {
        List<MRMSOption> mrmsOptions = mockCampaignOptions.getMRMSOptions();
        MRMSOption mrm = new MRMSOption(partRepairType, true, skillMin, skillMax, bthMin, bthMax, dailyTimeMin);
        mrmsOptions.add(mrm);
        when(mockCampaignOptions.getMRMSOptions()).thenReturn(mrmsOptions);
    }

    /**
     * Creates an {@link Entity} from the given unit name by retrieving its information from the cache.
     *
     * <p>If the unit cannot be found or loaded, appropriate error logging occurs, and {@code null}
     * is returned.
     * </p>
     *
     * @param unitName The name of the unit to retrieve and parse.
     *
     * @return The {@link Entity} representing the unit, or {@code null} if the unit cannot be loaded.
     */
    private Entity createEntity(String unitName) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        if (mekSummary == null) {
            logger.error("Cannot find entry for {}", unitName);
            return null;
        }

        MekFileParser mekFileParser;

        try {
            mekFileParser = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName());
        } catch (Exception ex) {
            logger.error("Unable to load unit: {}", mekSummary.getEntryName(), ex);
            return null;
        }

        return mekFileParser.getEntity();
    }
}

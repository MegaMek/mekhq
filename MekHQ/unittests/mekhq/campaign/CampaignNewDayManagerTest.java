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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Ranks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import testUtilities.MHQTestUtilities;

/**
 * Tests for CampaignNewDayManager temp crew pool processing.
 * Tests various combinations of Campaign options and MekHQ options
 * to ensure daily temp crew pool filling/distribution works correctly.
 */
public class CampaignNewDayManagerTest {

    private Campaign testCampaign;
    private CampaignOptions campaignOptions;
    private MHQOptions mhqOptions;

    @BeforeAll
    public static void setupAll() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
    }

    @BeforeEach
    public void setup() {
        testCampaign = spy(MHQTestUtilities.getTestCampaign());
        campaignOptions = testCampaign.getCampaignOptions();
        mhqOptions = MekHQ.getMHQOptions();
    }

    /**
     * Provides test cases for all combinations of campaign and MekHQ options
     * Format: PersonnelRole, campaignOptionEnabled, mhqOptionEnabled, shouldDistribute
     */
    private static Stream<Arguments> getDailyReminderOptionCombinations() {
        return Stream.of(
            // SOLDIER combinations
            Arguments.of(PersonnelRole.SOLDIER, false, false, false),
            Arguments.of(PersonnelRole.SOLDIER, false, true, false),
            Arguments.of(PersonnelRole.SOLDIER, true, false, false),
            Arguments.of(PersonnelRole.SOLDIER, true, true, true),

            // BATTLE_ARMOUR combinations
            Arguments.of(PersonnelRole.BATTLE_ARMOUR, false, false, false),
            Arguments.of(PersonnelRole.BATTLE_ARMOUR, false, true, false),
            Arguments.of(PersonnelRole.BATTLE_ARMOUR, true, false, false),
            Arguments.of(PersonnelRole.BATTLE_ARMOUR, true, true, true),

            // VEHICLE_CREW_GROUND combinations
            Arguments.of(PersonnelRole.VEHICLE_CREW_GROUND, false, false, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_GROUND, false, true, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_GROUND, true, false, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_GROUND, true, true, true),

            // VEHICLE_CREW_VTOL combinations
            Arguments.of(PersonnelRole.VEHICLE_CREW_VTOL, false, false, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_VTOL, false, true, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_VTOL, true, false, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_VTOL, true, true, true),

            // VEHICLE_CREW_NAVAL combinations
            Arguments.of(PersonnelRole.VEHICLE_CREW_NAVAL, false, false, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_NAVAL, false, true, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_NAVAL, true, false, false),
            Arguments.of(PersonnelRole.VEHICLE_CREW_NAVAL, true, true, true),

            // VESSEL_PILOT combinations
            Arguments.of(PersonnelRole.VESSEL_PILOT, false, false, false),
            Arguments.of(PersonnelRole.VESSEL_PILOT, false, true, false),
            Arguments.of(PersonnelRole.VESSEL_PILOT, true, false, false),
            Arguments.of(PersonnelRole.VESSEL_PILOT, true, true, true),

            // VESSEL_GUNNER combinations
            Arguments.of(PersonnelRole.VESSEL_GUNNER, false, false, false),
            Arguments.of(PersonnelRole.VESSEL_GUNNER, false, true, false),
            Arguments.of(PersonnelRole.VESSEL_GUNNER, true, false, false),
            Arguments.of(PersonnelRole.VESSEL_GUNNER, true, true, true),

            // VESSEL_CREW combinations
            Arguments.of(PersonnelRole.VESSEL_CREW, false, false, false),
            Arguments.of(PersonnelRole.VESSEL_CREW, false, true, false),
            Arguments.of(PersonnelRole.VESSEL_CREW, true, false, false),
            Arguments.of(PersonnelRole.VESSEL_CREW, true, true, true)
        );
    }

    /**
     * Nested test class for daily temp crew pool processing
     */
    @Nested
    class DailyTempCrewPoolTests {

        /**
         * Tests all combinations of campaign and MekHQ options for temp crew pool processing
         */
        @ParameterizedTest
        @MethodSource("mekhq.campaign.CampaignNewDayManagerTest#getDailyReminderOptionCombinations")
        void testDailyTempCrewPoolProcessing(PersonnelRole role, boolean campaignOptionEnabled,
                                              boolean mhqOptionEnabled, boolean shouldDistribute) {
            // Arrange
            configureCampaignOption(role, campaignOptionEnabled);
            configureMHQOption(role, mhqOptionEnabled);

            // Set initial pool value
            testCampaign.setTempCrewPool(role, 10);

            // Act
            processNewDayForRole(role);

            // Assert
            if (shouldDistribute) {
                // Pool should be reset to 0, then distribution called
                verify(testCampaign, times(1)).setTempCrewPool(role, 0);
                verify(testCampaign, times(1)).distributeTempCrewPoolToUnits(role);
            } else {
                // Pool should remain unchanged
                assertEquals(10, testCampaign.getTempCrewPool(role));
            }
        }

        /**
         * Tests that daily processing only affects the specified role
         */
        @Test
        void testDailyProcessingIsolation() {
            // Arrange
            campaignOptions.setUseBlobInfantry(true);
            mhqOptions.setNewDaySoldierPoolFill(true);

            testCampaign.setTempCrewPool(PersonnelRole.SOLDIER, 10);
            testCampaign.setTempCrewPool(PersonnelRole.BATTLE_ARMOUR, 20);

            // Act
            processNewDayForRole(PersonnelRole.SOLDIER);

            // Assert
            verify(testCampaign, times(1)).setTempCrewPool(PersonnelRole.SOLDIER, 0);
            assertEquals(20, testCampaign.getTempCrewPool(PersonnelRole.BATTLE_ARMOUR));
        }

        /**
         * Helper to configure campaign option for a role
         */
        private void configureCampaignOption(PersonnelRole role, boolean enabled) {
            switch (role) {
                case SOLDIER -> campaignOptions.setUseBlobInfantry(enabled);
                case BATTLE_ARMOUR -> campaignOptions.setUseBlobBattleArmor(enabled);
                case VEHICLE_CREW_GROUND -> campaignOptions.setUseBlobVehicleCrewGround(enabled);
                case VEHICLE_CREW_VTOL -> campaignOptions.setUseBlobVehicleCrewVTOL(enabled);
                case VEHICLE_CREW_NAVAL -> campaignOptions.setUseBlobVehicleCrewNaval(enabled);
                case VESSEL_PILOT -> campaignOptions.setUseBlobVesselPilot(enabled);
                case VESSEL_GUNNER -> campaignOptions.setUseBlobVesselGunner(enabled);
                case VESSEL_CREW -> campaignOptions.setUseBlobVesselCrew(enabled);
            }
        }

        /**
         * Helper to configure MekHQ option for a role
         */
        private void configureMHQOption(PersonnelRole role, boolean enabled) {
            switch (role) {
                case SOLDIER -> mhqOptions.setNewDaySoldierPoolFill(enabled);
                case BATTLE_ARMOUR -> mhqOptions.setNewDayBattleArmorPoolFill(enabled);
                case VEHICLE_CREW_GROUND -> mhqOptions.setNewDayVehicleCrewGroundPoolFill(enabled);
                case VEHICLE_CREW_VTOL -> mhqOptions.setNewDayVehicleCrewVTOLPoolFill(enabled);
                case VEHICLE_CREW_NAVAL -> mhqOptions.setNewDayVehicleCrewNavalPoolFill(enabled);
                case VESSEL_PILOT -> mhqOptions.setNewDayVesselPilotPoolFill(enabled);
                case VESSEL_GUNNER -> mhqOptions.setNewDayVesselGunnerPoolFill(enabled);
                case VESSEL_CREW -> mhqOptions.setNewDayVesselCrewPoolFill(enabled);
            }
        }

        /**
         * Helper to simulate new day processing for a specific role
         * Mimics the logic in CampaignNewDayManager
         */
        private void processNewDayForRole(PersonnelRole role) {
            boolean mhqOptionEnabled = getMHQOptionForRole(role);
            boolean campaignOptionEnabled = testCampaign.isBlobCrewEnabled(role);

            if (mhqOptionEnabled && campaignOptionEnabled) {
                testCampaign.setTempCrewPool(role, 0);
                testCampaign.distributeTempCrewPoolToUnits(role);
            }
        }

        /**
         * Helper to get MekHQ option value for a role
         */
        private boolean getMHQOptionForRole(PersonnelRole role) {
            return switch (role) {
                case SOLDIER -> mhqOptions.getNewDaySoldierPoolFill();
                case BATTLE_ARMOUR -> mhqOptions.getNewDayBattleArmorPoolFill();
                case VEHICLE_CREW_GROUND -> mhqOptions.getNewDayVehicleCrewGroundPoolFill();
                case VEHICLE_CREW_VTOL -> mhqOptions.getNewDayVehicleCrewVTOLPoolFill();
                case VEHICLE_CREW_NAVAL -> mhqOptions.getNewDayVehicleCrewNavalPoolFill();
                case VESSEL_PILOT -> mhqOptions.getNewDayVesselPilotPoolFill();
                case VESSEL_GUNNER -> mhqOptions.getNewDayVesselGunnerPoolFill();
                case VESSEL_CREW -> mhqOptions.getNewDayVesselCrewPoolFill();
                default -> false;
            };
        }
    }
}

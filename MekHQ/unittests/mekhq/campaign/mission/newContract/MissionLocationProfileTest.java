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
package mekhq.campaign.mission.newContract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.enums.AtBContractType;
import org.junit.jupiter.api.Test;

/**
 * Tests the contract-type-to-location-profile mapping. The groupings encode where each kind of operation happens in the
 * fiction (rear areas for training, deep strikes for raids, occupied territory for guerrillas...), so a change here
 * should be a conscious design decision, not a side effect.
 */
class MissionLocationProfileTest {

    @Test
    void rearAreaTypesMapToRearArea() {
        assertEquals(MissionLocationProfile.REAR_AREA,
              AtBContractType.CADRE_DUTY.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.REAR_AREA,
              AtBContractType.RETAINER.getMissionLocationProfile());
    }

    @Test
    void internalSecurityTypesMapToInteriorPopulated() {
        assertEquals(MissionLocationProfile.INTERIOR_POPULATED,
              AtBContractType.RIOT_DUTY.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.INTERIOR_POPULATED,
              AtBContractType.SECURITY_DUTY.getMissionLocationProfile());
    }

    @Test
    void raidTypesMapToDeepRaid() {
        assertEquals(MissionLocationProfile.DEEP_RAID,
              AtBContractType.DIVERSIONARY_RAID.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.DEEP_RAID,
              AtBContractType.OBJECTIVE_RAID.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.DEEP_RAID,
              AtBContractType.RECON_RAID.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.DEEP_RAID,
              AtBContractType.EXTRACTION_RAID.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.DEEP_RAID,
              AtBContractType.OBSERVATION_RAID.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.DEEP_RAID,
              AtBContractType.ASSASSINATION.getMissionLocationProfile());
    }

    @Test
    void guerrillaWarfareMapsToOccupiedTerritory() {
        assertEquals(MissionLocationProfile.OCCUPIED_TERRITORY,
              AtBContractType.GUERRILLA_WARFARE.getMissionLocationProfile());
    }

    @Test
    void covertHighValueTypesMapToHighValue() {
        assertEquals(MissionLocationProfile.HIGH_VALUE,
              AtBContractType.ESPIONAGE.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.HIGH_VALUE,
              AtBContractType.SABOTAGE.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.HIGH_VALUE,
              AtBContractType.TERRORISM.getMissionLocationProfile());
    }

    @Test
    void planetaryAssaultMapsToInvasion() {
        assertEquals(MissionLocationProfile.INVASION,
              AtBContractType.PLANETARY_ASSAULT.getMissionLocationProfile());
    }

    @Test
    void frontLineAndSpeciallyRoutedTypesMapToDefault() {
        assertEquals(MissionLocationProfile.DEFAULT,
              AtBContractType.GARRISON_DUTY.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.DEFAULT,
              AtBContractType.RELIEF_DUTY.getMissionLocationProfile());
        // Pirate hunting is routed by the enemy faction (PIR/BAN) instead, see PirateMissionTargetFinder.
        assertEquals(MissionLocationProfile.DEFAULT,
              AtBContractType.PIRATE_HUNTING.getMissionLocationProfile());
        // Deprecated for removal, and its location roles are inverted; see fromContractType's javadoc.
        assertEquals(MissionLocationProfile.DEFAULT,
              AtBContractType.MOLE_HUNTING.getMissionLocationProfile());
        assertEquals(MissionLocationProfile.DEFAULT,
              AtBContractType.UNDEFINED.getMissionLocationProfile());
    }

    @Test
    void onlyHighValueInteriorPopulatedAndInvasionArePopulationWeighted() {
        for (MissionLocationProfile profile : MissionLocationProfile.values()) {
            if ((profile == MissionLocationProfile.HIGH_VALUE) ||
                      (profile == MissionLocationProfile.INTERIOR_POPULATED) ||
                      (profile == MissionLocationProfile.INVASION)) {
                assertTrue(profile.isPopulationWeighted(), profile + " should be population-weighted");
            } else {
                assertFalse(profile.isPopulationWeighted(), profile + " should not be population-weighted");
            }
        }
    }

    /**
     * Only HIGH_VALUE and INVASION target worlds worth conquering or sabotaging for their industry; INTERIOR_POPULATED
     * cares only about the population (a riot doesn't care whether the crowd is at a factory or a farm).
     */
    @Test
    void onlyHighValueAndInvasionAreIndustriallyWeighted() {
        for (MissionLocationProfile profile : MissionLocationProfile.values()) {
            if ((profile == MissionLocationProfile.HIGH_VALUE) || (profile == MissionLocationProfile.INVASION)) {
                assertTrue(profile.isIndustriallyWeighted(), profile + " should be industrially weighted");
            } else {
                assertFalse(profile.isIndustriallyWeighted(), profile + " should not be industrially weighted");
            }
        }
    }
}

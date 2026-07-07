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
              MissionLocationProfile.fromContractType(AtBContractType.CADRE_DUTY));
        assertEquals(MissionLocationProfile.REAR_AREA,
              MissionLocationProfile.fromContractType(AtBContractType.RETAINER));
    }

    @Test
    void internalSecurityTypesMapToInteriorPopulated() {
        assertEquals(MissionLocationProfile.INTERIOR_POPULATED,
              MissionLocationProfile.fromContractType(AtBContractType.RIOT_DUTY));
        assertEquals(MissionLocationProfile.INTERIOR_POPULATED,
              MissionLocationProfile.fromContractType(AtBContractType.SECURITY_DUTY));
    }

    @Test
    void raidTypesMapToDeepRaid() {
        assertEquals(MissionLocationProfile.DEEP_RAID,
              MissionLocationProfile.fromContractType(AtBContractType.DIVERSIONARY_RAID));
        assertEquals(MissionLocationProfile.DEEP_RAID,
              MissionLocationProfile.fromContractType(AtBContractType.OBJECTIVE_RAID));
        assertEquals(MissionLocationProfile.DEEP_RAID,
              MissionLocationProfile.fromContractType(AtBContractType.RECON_RAID));
        assertEquals(MissionLocationProfile.DEEP_RAID,
              MissionLocationProfile.fromContractType(AtBContractType.EXTRACTION_RAID));
        assertEquals(MissionLocationProfile.DEEP_RAID,
              MissionLocationProfile.fromContractType(AtBContractType.OBSERVATION_RAID));
        assertEquals(MissionLocationProfile.DEEP_RAID,
              MissionLocationProfile.fromContractType(AtBContractType.ASSASSINATION));
    }

    @Test
    void guerrillaWarfareMapsToOccupiedTerritory() {
        assertEquals(MissionLocationProfile.OCCUPIED_TERRITORY,
              MissionLocationProfile.fromContractType(AtBContractType.GUERRILLA_WARFARE));
    }

    @Test
    void covertHighValueTypesMapToHighValue() {
        assertEquals(MissionLocationProfile.HIGH_VALUE,
              MissionLocationProfile.fromContractType(AtBContractType.ESPIONAGE));
        assertEquals(MissionLocationProfile.HIGH_VALUE,
              MissionLocationProfile.fromContractType(AtBContractType.SABOTAGE));
        assertEquals(MissionLocationProfile.HIGH_VALUE,
              MissionLocationProfile.fromContractType(AtBContractType.TERRORISM));
    }

    @Test
    void planetaryAssaultMapsToInvasion() {
        assertEquals(MissionLocationProfile.INVASION,
              MissionLocationProfile.fromContractType(AtBContractType.PLANETARY_ASSAULT));
    }

    @Test
    void frontLineAndSpeciallyRoutedTypesMapToDefault() {
        assertEquals(MissionLocationProfile.DEFAULT,
              MissionLocationProfile.fromContractType(AtBContractType.GARRISON_DUTY));
        assertEquals(MissionLocationProfile.DEFAULT,
              MissionLocationProfile.fromContractType(AtBContractType.RELIEF_DUTY));
        // Pirate hunting is routed by the enemy faction (PIR/BAN) instead, see PirateMissionTargetFinder.
        assertEquals(MissionLocationProfile.DEFAULT,
              MissionLocationProfile.fromContractType(AtBContractType.PIRATE_HUNTING));
        // Deprecated for removal, and its location roles are inverted; see fromContractType's javadoc.
        assertEquals(MissionLocationProfile.DEFAULT,
              MissionLocationProfile.fromContractType(AtBContractType.MOLE_HUNTING));
        assertEquals(MissionLocationProfile.DEFAULT,
              MissionLocationProfile.fromContractType(AtBContractType.UNDEFINED));
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

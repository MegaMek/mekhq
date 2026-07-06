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

import mekhq.campaign.mission.enums.AtBContractType;
import org.junit.jupiter.api.Test;

/**
 * Tests the contract-type-to-enemy-profile mapping. The groupings encode who each kind of operation is fought against
 * in the fiction (rebels for riots, raiders for rear-area postings, actual belligerents for assaults...), so a change
 * here should be a conscious design decision, not a side effect.
 */
class EnemySelectionProfileTest {

    @Test
    void pirateHuntingMapsToPirates() {
        assertEquals(EnemySelectionProfile.PIRATES,
              EnemySelectionProfile.fromContractType(AtBContractType.PIRATE_HUNTING));
    }

    @Test
    void riotDutyMapsToRebels() {
        assertEquals(EnemySelectionProfile.REBELS,
              EnemySelectionProfile.fromContractType(AtBContractType.RIOT_DUTY));
    }

    @Test
    void cadreDutyMapsToRaiders() {
        assertEquals(EnemySelectionProfile.RAIDERS,
              EnemySelectionProfile.fromContractType(AtBContractType.CADRE_DUTY));
    }

    @Test
    void openWarfareTypesMapToAtWar() {
        assertEquals(EnemySelectionProfile.AT_WAR,
              EnemySelectionProfile.fromContractType(AtBContractType.PLANETARY_ASSAULT));
        assertEquals(EnemySelectionProfile.AT_WAR,
              EnemySelectionProfile.fromContractType(AtBContractType.RELIEF_DUTY));
    }

    @Test
    void guerrillaWarfareMapsToOccupyingPower() {
        assertEquals(EnemySelectionProfile.OCCUPYING_POWER,
              EnemySelectionProfile.fromContractType(AtBContractType.GUERRILLA_WARFARE));
    }

    @Test
    void covertTypesMapToCovert() {
        assertEquals(EnemySelectionProfile.COVERT,
              EnemySelectionProfile.fromContractType(AtBContractType.ESPIONAGE));
        assertEquals(EnemySelectionProfile.COVERT,
              EnemySelectionProfile.fromContractType(AtBContractType.SABOTAGE));
        assertEquals(EnemySelectionProfile.COVERT,
              EnemySelectionProfile.fromContractType(AtBContractType.TERRORISM));
        assertEquals(EnemySelectionProfile.COVERT,
              EnemySelectionProfile.fromContractType(AtBContractType.ASSASSINATION));
    }

    @Test
    void remainingTypesMapToDefault() {
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.GARRISON_DUTY));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.SECURITY_DUTY));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.RETAINER));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.DIVERSIONARY_RAID));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.OBJECTIVE_RAID));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.RECON_RAID));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.EXTRACTION_RAID));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.OBSERVATION_RAID));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.MOLE_HUNTING));
        assertEquals(EnemySelectionProfile.DEFAULT,
              EnemySelectionProfile.fromContractType(AtBContractType.UNDEFINED));
    }
}

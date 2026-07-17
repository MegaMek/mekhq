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
              AtBContractType.PIRATE_HUNTING.getEnemySelectionProfile());
    }

    @Test
    void riotDutyMapsToRebels() {
        assertEquals(EnemySelectionProfile.REBELS,
              AtBContractType.RIOT_DUTY.getEnemySelectionProfile());
    }

    @Test
    void cadreDutyMapsToRaiders() {
        assertEquals(EnemySelectionProfile.RAIDERS,
              AtBContractType.CADRE_DUTY.getEnemySelectionProfile());
    }

    @Test
    void openWarfareTypesMapToAtWar() {
        assertEquals(EnemySelectionProfile.AT_WAR,
              AtBContractType.PLANETARY_ASSAULT.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.AT_WAR,
              AtBContractType.RELIEF_DUTY.getEnemySelectionProfile());
    }

    @Test
    void guerrillaWarfareMapsToOccupyingPower() {
        assertEquals(EnemySelectionProfile.OCCUPYING_POWER,
              AtBContractType.GUERRILLA_WARFARE.getEnemySelectionProfile());
    }

    @Test
    void covertTypesMapToCovert() {
        assertEquals(EnemySelectionProfile.COVERT,
              AtBContractType.ESPIONAGE.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.COVERT,
              AtBContractType.SABOTAGE.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.COVERT,
              AtBContractType.TERRORISM.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.COVERT,
              AtBContractType.ASSASSINATION.getEnemySelectionProfile());
    }

    @Test
    void remainingTypesMapToDefault() {
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.GARRISON_DUTY.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.SECURITY_DUTY.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.RETAINER.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.DIVERSIONARY_RAID.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.OBJECTIVE_RAID.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.RECON_RAID.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.EXTRACTION_RAID.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.OBSERVATION_RAID.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.MOLE_HUNTING.getEnemySelectionProfile());
        assertEquals(EnemySelectionProfile.DEFAULT,
              AtBContractType.UNDEFINED.getEnemySelectionProfile());
    }
}

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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertSame;

import mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.targetFinder.EnemySelectionProfile;
import mekhq.campaign.mission.contract.contractGeneration.targetFinder.MissionLocationProfile;
import org.junit.jupiter.api.Test;

/**
 * Tests the wiring of the {@link ContractObjectiveType#PIRATE_RAID} objective: its strategic (Chaos) category, its
 * victim-selection profile, and where the raid is situated.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ContractObjectiveTypeTest {

    @Test
    void pirateRaidMapsToThePirateRaidChaosObjective() {
        assertSame(ChaosObjectiveType.PIRATE_RAID, ContractObjectiveType.PIRATE_RAID.getChaosObjectiveType(),
              "a pirate raid must use its own strategic profile, not the plain raid profile");
    }

    @Test
    void pirateRaidTargetsAVictim() {
        assertSame(EnemySelectionProfile.PIRATE_VICTIM, ContractObjectiveType.PIRATE_RAID.getEnemySelectionProfile(),
              "a pirate raid draws a victim rather than a conventional belligerent");
    }

    @Test
    void pirateRaidStrikesHighValueRegions() {
        assertSame(MissionLocationProfile.HIGH_VALUE, ContractObjectiveType.PIRATE_RAID.getMissionLocationProfile(),
              "a pirate raid is drawn toward valuable regions (the soft target within is chosen later)");
    }
}

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

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.units.IBomber;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * "Unload All Bombs" returns every bomb on an aircraft to the warehouse in one step (issue #2604).
 *
 * <p>The Batu Prime is a Clan OmniFighter, so it can carry external bombs.</p>
 */
class BombUnloaderTest {
    private static final int HIGH_EXPLOSIVE_BOMBS = 2;
    private static final int CLUSTER_BOMBS = 1;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit batu;
    private IBomber bomber;
    private AmmoType highExplosiveAmmo;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        batu = scenario.withUnit(UnitFixture.BATU_PRIME);
        bomber = (IBomber) batu.getEntity();
        highExplosiveAmmo = (AmmoType) EquipmentType.get(BombTypeEnum.HE.getInternalName());
    }

    /**
     * Counts the high-explosive bombs in the warehouse. The quartermaster's ammunition count adds every bomb type
     * together, because they share one ammunition family, so the stacks are counted here by their exact type.
     */
    private int highExplosiveBombsInStock() {
        int bombs = 0;
        for (Part part : campaign.getPlayerForce().getWarehouse().getParts()) {
            if (!(part instanceof AmmoStorage storage)) {
                continue;
            }
            if (storage.getType() == highExplosiveAmmo) {
                bombs += storage.getShots();
            }
        }
        return bombs;
    }

    private void loadBombs() {
        BombLoadout loadout = new BombLoadout();
        loadout.put(BombTypeEnum.HE, HIGH_EXPLOSIVE_BOMBS);
        loadout.put(BombTypeEnum.CLUSTER, CLUSTER_BOMBS);
        bomber.setBombChoices(loadout);
        assertEquals(HIGH_EXPLOSIVE_BOMBS + CLUSTER_BOMBS, bomber.getBombChoices().getTotalBombs());
    }

    @Test
    void anAircraftWithoutBombsHasNothingToUnload() {
        assertFalse(BombUnloader.hasBombsToUnload(batu));
        assertFalse(BombUnloader.hasBombsToUnload(scenario.withUnit(UnitFixture.LOCUST_LCT_1V)),
              "A Mek is not a bomber");
    }

    @Test
    void everyBombGoesBackToTheWarehouseAndTheAircraftIsClean() {
        loadBombs();
        assertTrue(BombUnloader.hasBombsToUnload(batu));

        int bombsReturned = BombUnloader.unloadAll(campaign, batu);

        assertEquals(HIGH_EXPLOSIVE_BOMBS + CLUSTER_BOMBS, bombsReturned);
        assertEquals(0, bomber.getBombChoices().getTotalBombs(), "The Batu is clean");
        assertEquals(HIGH_EXPLOSIVE_BOMBS, highExplosiveBombsInStock());
        assertFalse(BombUnloader.hasBombsToUnload(batu));
    }

    @Test
    void returnedBombsJoinTheBombsAlreadyInStock() {
        campaign.getQuartermaster().addAmmo(highExplosiveAmmo, 3);
        loadBombs();

        BombUnloader.unloadAll(campaign, batu);

        assertEquals(3 + HIGH_EXPLOSIVE_BOMBS, highExplosiveBombsInStock());
    }
}

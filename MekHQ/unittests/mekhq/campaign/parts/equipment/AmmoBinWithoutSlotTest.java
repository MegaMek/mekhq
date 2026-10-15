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
package mekhq.campaign.parts.equipment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * An ammo bin that lost its slot on the unit, as a salvaged bin whose munition no longer matched could, is refused
 * with a reason. Before, its reload never filled it, so the job ran again every day until the techs ran out of time
 * (issue #7912).
 */
class AmmoBinWithoutSlotTest {
    private static final int SPARE_TONS = 2;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit wolverine;
    private AmmoBin ammunitionBin;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        ammunitionBin = PartsScenario.unitParts(wolverine, AmmoBin.class).getFirst();
        ammunitionBin.setShotsNeeded(ammunitionBin.getFullShots());
        scenario.withSpare(new AmmoStorage(0, ammunitionBin.getType(), ammunitionBin.getType().getShots(), campaign),
              SPARE_TONS);
    }

    @Test
    void aBinWithItsSlotCanBeReloaded() {
        assertNull(ammunitionBin.checkFixable(), "The empty bin can be reloaded from the spare ammunition");
    }

    @Test
    void aBinWithNoSlotIsRefusedWithAReason() {
        ammunitionBin.setEquipmentNum(-1);

        assertNotNull(ammunitionBin.checkFixable());
    }

    @Test
    void aMissingBinWithNoSlotIsRefusedWithAReason() {
        MissingAmmoBin missingBin = new MissingAmmoBin(0, ammunitionBin.getType(), -1, false, false,
              campaign);
        missingBin.setUnit(wolverine);

        assertNotNull(missingBin.checkFixable());
    }
}

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

import java.util.List;

import megamek.common.units.Aero;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Heat sink parts for the sinks a fighter's fusion engine provides leave the campaign when the fighter's parts are set
 * up, instead of staying in the warehouse tied to the fighter and turning into free spares once it is sold (issue
 * #10353).
 */
class FighterHeatSinkLeftoversTest {
    private Campaign campaign;
    private LocalWarehouse warehouse;
    private Unit fighter;
    private int heatSinkPartsNeeded;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        warehouse = campaign.getPlayerForce().getWarehouse();
        fighter = scenario.withUnit(UnitFixture.SHILONE_SL_17);
        Aero aero = (Aero) fighter.getEntity();
        heatSinkPartsNeeded = PartsScenario.unitParts(fighter, AeroHeatSink.class).size();
        // Parts for the ten engine-provided sinks, as an older version left them
        for (int index = 0; index < 10; index++) {
            AeroHeatSink leftover = new AeroHeatSink((int) aero.getWeight(), aero.getHeatType(), false, campaign);
            leftover.setUnit(fighter);
            warehouse.addPart(leftover, false);
            fighter.addPart(leftover);
        }
    }

    private int spareAeroHeatSinks() {
        int spareCount = 0;
        for (Part part : warehouse.getParts()) {
            if ((part instanceof AeroHeatSink) && (part.getUnit() == null)) {
                spareCount += part.getQuantity();
            }
        }
        return spareCount;
    }

    @Test
    void settingUpTheFightersPartsTakesTheLeftoversOutOfTheCampaign() {
        fighter.initializeParts(false);

        assertEquals(heatSinkPartsNeeded, PartsScenario.unitParts(fighter, AeroHeatSink.class).size());
        for (Part part : warehouse.getParts()) {
            boolean isTiedToTheFighter = part.getUnit() == fighter;
            assertFalse(isTiedToTheFighter && !fighter.getParts().contains(part),
                  "No part is left in the warehouse tied to the fighter without being on it");
        }
    }

    @Test
    void sellingTheFighterLeavesNoFreeHeatSinksBehind() {
        fighter.initializeParts(false);
        int sparesBefore = spareAeroHeatSinks();

        campaign.getQuartermaster().sellUnit(fighter);
        for (Part part : List.copyOf(warehouse.getParts())) {
            if (part.getUnit() == fighter) {
                // what a reload does to a part whose unit has gone: it becomes a spare
                part.setUnit(null);
            }
        }

        assertEquals(sparesBefore, spareAeroHeatSinks());
    }
}

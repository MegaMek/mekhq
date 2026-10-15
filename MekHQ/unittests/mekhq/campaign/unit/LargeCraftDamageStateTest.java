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

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import megamek.common.bays.Bay;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A new DropShip, JumpShip, WarShip or space station shows no damage in the hangar, and its bays and doors are intact,
 * both when it arrives and after the campaign is saved and loaded (issue #2056).
 */
class LargeCraftDamageStateTest {

    private static Unit saveAndLoad(Unit unit, Campaign campaign) throws Exception {
        StringWriter unitXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(unitXml);
        unit.writeToXML(printWriter, 0);
        printWriter.flush();
        Element unitElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          unitXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
        Unit loadedUnit = Unit.generateInstanceFromXML(unitElement, new Version(), campaign);
        loadedUnit.setCampaign(campaign);
        loadedUnit.initializeParts(false);
        loadedUnit.runDiagnostic(false);
        return loadedUnit;
    }

    private static void assertUndamaged(Unit unit, String when) {
        Entity entity = unit.getEntity();
        assertEquals(Entity.DMG_NONE, unit.getDamageState(), entity.getDisplayName() + " shows damage " + when);
        for (Bay bay : entity.getTransportBays()) {
            assertEquals(0.0, bay.getBayDamage(), "Bay " + bay.getBayNumber() + " is damaged " + when);
            assertEquals(bay.getDoors(), bay.getCurrentDoors(),
                  "Bay " + bay.getBayNumber() + " has doors missing " + when);
        }
    }

    @ParameterizedTest
    @EnumSource(value = UnitFixture.class, names = { "LEOPARD_DROPSHIP", "LEOPARD_DROPSHIP_3056",
                                                     "LEOPARD_CV_DROPSHIP", "LEOPARD_PA_DROPSHIP",
                                                     "INVADER_JUMPSHIP", "ESSEX_II_WARSHIP",
                                                     "OLYMPUS_SPACE_STATION" })
    void aNewLargeCraftShowsNoDamage(UnitFixture fixture) throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Unit unit = scenario.withUnit(fixture);

        assertUndamaged(unit, "when it arrives");
        assertUndamaged(saveAndLoad(unit, scenario.getCampaign()), "after a save and load");
    }
}

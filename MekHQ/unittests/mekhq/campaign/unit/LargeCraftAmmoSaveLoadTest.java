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

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import megamek.Version;
import megamek.common.equipment.AmmoMounted;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A DropShip, WarShip or space station arrives with every ammunition bin full, and keeps them full through a save and
 * load (issue #2466, where large craft once loaded with their ammunition missing).
 */
class LargeCraftAmmoSaveLoadTest {
    /**
     * Each bin on the unit, as its ammunition, location, shots and condition.
     */
    private static List<String> ammoLoadout(Entity entity) {
        List<String> loadout = new ArrayList<>();
        for (AmmoMounted ammo : entity.getAmmo()) {
            loadout.add(ammo.getType().getInternalName() + " in location " + ammo.getLocation() + ": "
                              + ammo.getBaseShotsLeft() + " shots, hit " + ammo.isHit() + ", destroyed "
                              + ammo.isDestroyed() + ", missing " + ammo.isMissing());
        }
        return loadout;
    }

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

    @ParameterizedTest
    @EnumSource(value = UnitFixture.class, names = { "LEOPARD_DROPSHIP", "ESSEX_II_WARSHIP",
                                                    "OLYMPUS_SPACE_STATION" })
    void largeCraftArriveWithEveryAmmoBinFull(UnitFixture fixture) {
        Unit unit = PartsScenario.create().withUnit(fixture);
        List<AmmoBin> ammoBins = PartsScenario.unitParts(unit, AmmoBin.class);

        assertFalse(ammoBins.isEmpty(), fixture + " carries ammunition");
        assertEquals(unit.getEntity().getAmmo().size(), ammoBins.size(), "One bin for each ammunition slot");
        for (AmmoBin ammoBin : ammoBins) {
            assertEquals(0, ammoBin.getShotsNeeded(), ammoBin.getName() + " is full");
            assertFalse(ammoBin.needsFixing(), ammoBin.getName() + " is undamaged");
        }
    }

    @ParameterizedTest
    @EnumSource(value = UnitFixture.class, names = { "LEOPARD_DROPSHIP", "ESSEX_II_WARSHIP",
                                                    "OLYMPUS_SPACE_STATION" })
    void largeCraftKeepTheirAmmunitionThroughASaveAndLoad(UnitFixture fixture) throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Unit unit = scenario.withUnit(fixture);
        List<String> loadoutBeforeSaving = ammoLoadout(unit.getEntity());
        int binCountBeforeSaving = PartsScenario.unitParts(unit, AmmoBin.class).size();

        Unit loadedUnit = saveAndLoad(unit, scenario.getCampaign());

        assertEquals(loadoutBeforeSaving, ammoLoadout(loadedUnit.getEntity()));
        List<AmmoBin> loadedBins = PartsScenario.unitParts(loadedUnit, AmmoBin.class);
        assertEquals(binCountBeforeSaving, loadedBins.size(), "Every ammunition bin is still there");
        for (AmmoBin ammoBin : loadedBins) {
            assertEquals(0, ammoBin.getShotsNeeded(), ammoBin.getName() + " is still full");
        }
    }
}

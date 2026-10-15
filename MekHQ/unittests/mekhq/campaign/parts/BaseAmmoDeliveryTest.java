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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Ammunition ordered for a bin on a unit at a base is delivered to the base, and the bin counts and loads the base's
 * stock (issue #10352).
 */
class BaseAmmoDeliveryTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private LocalWarehouse mainWarehouse;
    private PlayerBase base;
    private LocalWarehouse baseWarehouse;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        baseWarehouse = base.getBaseWarehouse();
        campaign.getPlayerForce().getFinances().credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(),
              Money.of(10_000_000), "Funds for ammunition");
    }

    /** A Locust kept at the base with its machine gun bin emptied, and no spare ammunition anywhere. */
    private AmmoBin emptyBinAtBase() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        campaign.getPlayerForce().getHangar().removeUnit(locust.getId());
        base.getBaseHangar().addUnit(locust);
        for (Part part : List.copyOf(locust.getParts())) {
            mainWarehouse.removePart(part);
            baseWarehouse.addPart(part);
        }
        AmmoBin bin = PartsScenario.unitParts(locust, AmmoBin.class).getFirst();
        bin.unload();
        removeSpareAmmunition(mainWarehouse);
        removeSpareAmmunition(baseWarehouse);
        return bin;
    }

    private static void removeSpareAmmunition(LocalWarehouse warehouse) {
        for (Part part : List.copyOf(warehouse.getParts())) {
            if ((part instanceof AmmoStorage) && (part.getUnit() == null)) {
                warehouse.removePart(part);
            }
        }
    }

    private static int spareShots(LocalWarehouse warehouse, AmmoBin bin) {
        int shots = 0;
        for (Part part : warehouse.getParts()) {
            boolean isSpareOfThisType = (part instanceof AmmoStorage ammo) && (part.getUnit() == null)
                                              && ammo.getType().equals(bin.getType());
            if (isSpareOfThisType) {
                shots += ((AmmoStorage) part).getShots();
            }
        }
        return shots;
    }

    @Test
    void ammunitionOrderedForABinAtABaseIsDeliveredToTheBase() {
        AmmoBin bin = emptyBinAtBase();

        bin.getAcquisitionWork().find(0, 1.0);

        assertEquals(0, spareShots(mainWarehouse, bin), "Nothing arrives in the main force's warehouse");
        assertEquals(bin.getShotsNeeded(), spareShots(baseWarehouse, bin));
    }

    @Test
    void aBinAtABaseLoadsTheAmmunitionDeliveredForIt() {
        AmmoBin bin = emptyBinAtBase();
        bin.getAcquisitionWork().find(0, 1.0);

        bin.loadBin();

        assertEquals(0, bin.getShotsNeeded());
    }

    @Test
    void aBinAtABaseDoesNotCountTheMainForcesAmmunition() {
        AmmoBin bin = emptyBinAtBase();
        AmmoStorage mainStock = bin.getNewPart();
        mainWarehouse.addPart(mainStock, false);

        assertFalse(bin.isAmmoAvailable());
        assertEquals(0, bin.getAmountAvailable());
    }

    @Test
    void anOrderKeepsTheUnitItIsForThroughASaveAndLoad() throws Exception {
        AmmoBin bin = emptyBinAtBase();
        AmmoStorage order = (AmmoStorage) bin.getAcquisitionWork();
        StringWriter orderXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(orderXml);
        order.writeToXML(printWriter, 0);
        printWriter.flush();

        AmmoStorage loadedOrder = (AmmoStorage) Part.generateInstanceFromXML(MHQXMLUtility.newSafeDocumentBuilder()
                                                                                    .parse(new ByteArrayInputStream(
                                                                                          orderXml.toString()
                                                                                                .getBytes(
                                                                                                      StandardCharsets.UTF_8)))
                                                                                    .getDocumentElement(),
              new Version());

        assertEquals(bin.getUnit().getId(), loadedOrder.getDeliveryUnitId());
    }

    @Test
    void ammunitionOrderedForABinInTheMainForceStillGoesToTheMainWarehouse() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        AmmoBin bin = PartsScenario.unitParts(locust, AmmoBin.class).getFirst();
        bin.unload();
        removeSpareAmmunition(mainWarehouse);

        bin.getAcquisitionWork().find(0, 1.0);

        assertEquals(bin.getShotsNeeded(), spareShots(mainWarehouse, bin));
        assertEquals(0, spareShots(baseWarehouse, bin));
    }
}

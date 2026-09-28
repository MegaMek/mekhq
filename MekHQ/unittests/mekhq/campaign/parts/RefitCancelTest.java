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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Cancelling a refit must take back exactly what the refit asked for: its kit and its own orders leave the procurement
 * list, the player's orders stay, and nothing the campaign never obtained turns up in the warehouse (issue #10193).
 */
class RefitCancelTest {
    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    private ForceShoppingList procurement() {
        return campaign.getPlayerForce().getShoppingList();
    }

    /** Counts the procurement list by acquisition name, adding up each order's quantity. */
    private SortedMap<String, Integer> procurementList() {
        SortedMap<String, Integer> census = new TreeMap<>();
        for (IAcquisitionWork order : procurement().getShoppingList()) {
            census.merge(order.getAcquisitionName(), order.getQuantity(), Integer::sum);
        }
        return census;
    }

    private Refit beginCustomLocustRefit() throws Exception {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), true, false, false);
        refit.begin();
        return refit;
    }

    @Test
    void cancelKeepsThePlayersOwnOrderOfTheSamePart() throws Exception {
        Refit refit = beginCustomLocustRefit();
        Part smallLaser = refit.getShoppingList()
                                .stream()
                                .map(entry -> (Part) ((IAcquisitionWork) entry).getNewEquipment())
                                .filter(newPart -> "Small Laser".equals(newPart.getName()))
                                .findFirst()
                                .orElseThrow();
        procurement().addShoppingItem(smallLaser.getAcquisitionWork(), 1, campaign);

        assertEquals(Map.of("Medium Laser", 1, "Small Laser", 3), procurementList(),
              "The player's Small Laser is a separate order from the refit's two");
        assertEquals(Map.of("Medium Laser", 1, "Small Laser", 2), PartsCensus.ofParts(refit.getShoppingList()));

        refit.cancel();

        assertEquals(Map.of("Small Laser", 1), procurementList(), "Only the player's own order is left");
    }

    @Test
    void cancelDoesNotCreateAmmoTheRefitWasStillToBuy() throws Exception {
        // The LCT-1V carries Machine Gun ammo the LCT-1E lacks, and the warehouse holds none, so the refit lists ammo
        // to buy
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1E);
        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1V.loadEntity(), false, false, false);
        refit.begin();
        boolean isWaitingForAmmo = refit.getShoppingList().stream().anyMatch(part -> part instanceof AmmoStorage);
        assertTrue(isWaitingForAmmo, "The refit is waiting to buy Machine Gun ammo");

        refit.cancel();

        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()),
              "The ammo was never bought, so none appears");
        assertEquals(Map.of(), procurementList());
    }

    @Test
    void refitOrdersReadBackFromASaveAreStillRemovedOnCancel() throws Exception {
        Refit refit = beginCustomLocustRefit();
        campaign.getPlayerForce().setShoppingList(writeAndReadBack(procurement()));
        assertEquals(Map.of("Medium Laser", 1, "Small Laser", 2), procurementList());

        refit.cancel();

        assertEquals(Map.of(), procurementList());
    }

    private ForceShoppingList writeAndReadBack(ForceShoppingList shoppingList) throws Exception {
        StringWriter xmlText = new StringWriter();
        try (PrintWriter xmlWriter = new PrintWriter(xmlText)) {
            shoppingList.writeToXML(xmlWriter, 0);
        }
        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                  .parse(new ByteArrayInputStream(xmlText.toString()
                                                                        .getBytes(StandardCharsets.UTF_8)));
        ForceShoppingList reloaded = ForceShoppingList.generateInstanceFromXML(document.getDocumentElement(), campaign,
              new Version());
        reloaded.restore();
        return reloaded;
    }
}

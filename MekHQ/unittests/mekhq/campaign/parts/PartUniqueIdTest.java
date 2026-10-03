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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import megamek.Version;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Every part has an identity that is unique across the campaign and survives a save. Warehouses number their parts
 * from 1, so the number alone cannot tell a base's part from the main force's (issue #10343).
 */
class PartUniqueIdTest {
    private Part mediumLaser;
    private PartsScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        mediumLaser = PartsScenario.unitParts(scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R), EquipmentPart.class)
                            .getFirst();
    }

    private static String saved(Part part) {
        StringWriter partXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(partXml);
        part.writeToXML(printWriter, 0);
        printWriter.flush();
        return partXml.toString();
    }

    private static Part loaded(String partXml) throws Exception {
        Element partElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(partXml.getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
        return Part.generateInstanceFromXML(partElement, new Version());
    }

    @Test
    void aCloneIsADifferentPart() {
        Part copy = mediumLaser.clone();

        assertNotNull(copy.getUniqueId());
        assertNotEquals(mediumLaser.getUniqueId(), copy.getUniqueId());
    }

    @Test
    void aPartKeepsItsIdentityThroughASaveAndLoad() throws Exception {
        Part loadedPart = loaded(saved(mediumLaser));

        assertEquals(mediumLaser.getUniqueId(), loadedPart.getUniqueId());
        assertEquals(mediumLaser.getId(), loadedPart.getId(), "The part number is kept for display");
    }

    @Test
    void aPartFromAnOlderSaveIsGivenAnIdentity() throws Exception {
        String olderSave = saved(mediumLaser).replaceAll("\\s*<uniqueId>[^<]*</uniqueId>", "");

        Part loadedPart = loaded(olderSave);

        assertNotNull(loadedPart.getUniqueId());
        assertNotEquals(mediumLaser.getUniqueId(), loadedPart.getUniqueId());
    }

    @Test
    void anUnreadableIdentityIsReplacedRatherThanStoppingTheLoad() throws Exception {
        String damagedSave = saved(mediumLaser).replaceAll("<uniqueId>[^<]*</uniqueId>", "<uniqueId>x</uniqueId>");

        Part loadedPart = loaded(damagedSave);

        assertNotNull(loadedPart);
        assertNotNull(loadedPart.getUniqueId());
    }

    @Test
    void aWarehouseFindsAPartByItsIdentity() {
        LocalWarehouse warehouse = new LocalWarehouse();
        Part spare = mediumLaser.clone();
        warehouse.addPart(spare);

        assertSame(spare, warehouse.getPart(spare.getUniqueId()));

        warehouse.removePart(spare);

        assertNull(warehouse.getPart(spare.getUniqueId()));
    }

    @Test
    void removingAPartNeverRemovesAnotherWarehousesPartWithTheSameNumber() {
        LocalWarehouse mainWarehouse = new LocalWarehouse();
        LocalWarehouse baseWarehouse = new LocalWarehouse();
        Part mainPart = mediumLaser.clone();
        Part basePart = mediumLaser.clone();
        mainWarehouse.addPart(mainPart);
        baseWarehouse.addPart(basePart);
        assertEquals(mainPart.getId(), basePart.getId(), "Both warehouses number from 1");

        boolean isRemoved = mainWarehouse.removePart(basePart);

        assertFalse(isRemoved);
        assertSame(mainPart, mainWarehouse.getPart(mainPart.getId()), "The main warehouse's part is still there");
        assertSame(basePart, baseWarehouse.getPart(basePart.getId()), "The base's part keeps its number");
        assertTrue(baseWarehouse.removePart(basePart), "Its own warehouse can still remove it");
    }

    @Test
    void askingTheWrongWarehouseToRemoveAPartLeavesItsNumberAlone() {
        LocalWarehouse mainWarehouse = scenario.getCampaign().getPlayerForce().getWarehouse();
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        scenario.getCampaign().getCampaignLocationManager().addPlayerBase(base);
        LocalWarehouse baseWarehouse = base.getBaseWarehouse();
        for (Part part : List.copyOf(mainWarehouse.getParts())) {
            mainWarehouse.removePart(part);
        }
        Part basePart = mediumLaser.clone();
        baseWarehouse.addPart(basePart);
        int number = basePart.getId();

        assertFalse(mainWarehouse.removePart(basePart), "The main warehouse has no part with this number");

        assertEquals(number, basePart.getId(), "The base's part keeps its number");
        assertSame(basePart, baseWarehouse.getPart(number));
    }
}

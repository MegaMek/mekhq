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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Links between parts survive a load, including at a base whose part numbers repeat the main force's (issue #10343).
 *
 * <p>The older-save cases rebuild the state a campaign load is in when links are fixed: parts held by a base
 * warehouse, linked by number, whose unit is not yet attached, and parts in the main warehouse that happen to carry
 * the same numbers.</p>
 */
class PartLinkResolverTest {
    private static final int BAY_NUMBER = 1;
    private static final int DOOR_NUMBER = 2;

    private Campaign campaign;
    private LocalWarehouse mainWarehouse;
    private LocalWarehouse baseWarehouse;
    private Part bayTemplate;
    private Part doorTemplate;
    private Part laserTemplate;
    private UUID baseUnitId;
    private UUID otherUnitId;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        bayTemplate = PartsScenario.unitParts(leopard, TransportBayPart.class).getFirst();
        doorTemplate = PartsScenario.unitParts(leopard, BayDoor.class).getFirst();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        laserTemplate = PartsScenario.unitParts(wolverine, EquipmentPart.class).getFirst();
        // the units the parts are on; which hangar holds them does not matter while links are fixed
        baseUnitId = leopard.getId();
        otherUnitId = wolverine.getId();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        for (Part part : List.copyOf(mainWarehouse.getParts())) {
            mainWarehouse.removePart(part);
        }
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        baseWarehouse = base.getBaseWarehouse();

        // Spares in the main warehouse that carry the numbers the base's parts use
        mainWarehouse.addPart(olderSavePart(laserTemplate, BAY_NUMBER, null, ""));
        mainWarehouse.addPart(olderSavePart(laserTemplate, DOOR_NUMBER, null, ""));
    }

    private static String saved(Part part) {
        StringWriter partXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(partXml);
        part.writeToXML(printWriter, 0);
        printWriter.flush();
        return partXml.toString();
    }

    private Part loaded(String partXml) throws Exception {
        Element partElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(partXml.getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
        Part part = Part.generateInstanceFromXML(partElement, new Version());
        part.setCampaign(campaign);
        return part;
    }

    /**
     * A part as an older save writes it: numbered, linked by number, and on the given unit or a spare.
     */
    private Part olderSavePart(Part template, int number, UUID unitId, String numberLinks) {
        String partXml = saved(template)
                               .replaceAll("<(unitId|replacementUniqueId|parentPartUniqueId|childPartUniqueId|"
                                                 + "reserveId|techId)>[^<]*</\\1>\\s*", "")
                               .replaceFirst("id=\"-?\\d+\"", "id=\"" + number + "\"")
                               .replaceFirst("<id>-?\\d+</id>", "<id>" + number + "</id>");
        String unitLink = (unitId == null) ? "" : ("<unitId>" + unitId + "</unitId>");
        try {
            return loaded(partXml.replace("</part>", unitLink + numberLinks + "</part>"));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void aBaseDoorFromAnOlderSaveFindsItsOwnBayAtTheBase() {
        Part bay = olderSavePart(bayTemplate, BAY_NUMBER, baseUnitId, "<childPartId>" + DOOR_NUMBER + "</childPartId>");
        Part door = olderSavePart(doorTemplate, DOOR_NUMBER, baseUnitId, "<parentPartId>" + BAY_NUMBER + "</parentPartId>");
        baseWarehouse.addPart(bay);
        baseWarehouse.addPart(door);

        door.fixReferences(campaign);
        bay.fixReferences(campaign);

        assertSame(bay, door.getParentPart(), "Not the main warehouse's part with the same number");
        assertEquals(List.of(door), bay.getChildParts());
    }

    @Test
    void aLinkThatTheOtherPartDoesNotShareIsDroppedRatherThanGuessed() {
        Part bay = olderSavePart(bayTemplate, BAY_NUMBER, baseUnitId, "");
        Part door = olderSavePart(doorTemplate, DOOR_NUMBER, baseUnitId, "<parentPartId>" + BAY_NUMBER + "</parentPartId>");
        baseWarehouse.addPart(bay);
        baseWarehouse.addPart(door);

        door.fixReferences(campaign);

        assertNull(door.getParentPart(), "The bay does not list this door, so the door is not tied to it");
    }

    @Test
    void aLinkToAPartOnAnotherUnitIsDropped() {
        Part bay = olderSavePart(bayTemplate, BAY_NUMBER, otherUnitId,
              "<childPartId>" + DOOR_NUMBER + "</childPartId>");
        Part door = olderSavePart(doorTemplate, DOOR_NUMBER, baseUnitId, "<parentPartId>" + BAY_NUMBER + "</parentPartId>");
        baseWarehouse.addPart(bay);
        baseWarehouse.addPart(door);

        door.fixReferences(campaign);

        assertNull(door.getParentPart());
    }

    @Test
    void aReplacementFromAnOlderSaveIsTheSpareReservedAtTheBase() {
        UUID techId = UUID.randomUUID();
        int spareNumber = BAY_NUMBER;
        Part reservedSpare = olderSavePart(laserTemplate, spareNumber, null, "<reserveId>" + techId + "</reserveId>");
        Part missingLaser = olderSavePart(laserTemplate.getMissingPart(), DOOR_NUMBER, baseUnitId,
              "<techId>" + techId + "</techId><replacementId>" + spareNumber + "</replacementId>");
        baseWarehouse.addPart(reservedSpare);
        baseWarehouse.addPart(missingLaser);

        missingLaser.fixReferences(campaign);

        assertSame(reservedSpare, missingLaser.getReplacementPart());
    }

    @Test
    void aLinkSavedByIdentityFindsItsPartInAnotherWarehouse() throws Exception {
        Part spareBay = olderSavePart(bayTemplate, BAY_NUMBER, null, "");
        Part spareDoor = olderSavePart(doorTemplate, DOOR_NUMBER, null, "");
        spareBay.addChildPart(spareDoor);
        Part loadedBay = loaded(saved(spareBay));
        Part loadedDoor = loaded(saved(spareDoor));
        baseWarehouse.addPart(loadedBay);
        mainWarehouse.addPart(loadedDoor);

        loadedBay.fixReferences(campaign);
        loadedDoor.fixReferences(campaign);

        assertTrue(saved(spareBay).contains("<childPartUniqueId>" + spareDoor.getUniqueId()));
        assertEquals(List.of(loadedDoor), loadedBay.getChildParts());
        assertSame(loadedBay, loadedDoor.getParentPart());
    }
}

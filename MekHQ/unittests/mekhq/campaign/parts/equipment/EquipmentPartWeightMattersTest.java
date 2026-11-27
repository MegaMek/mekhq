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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;

/**
 * Equipment whose type does not fix its weight, such as MASH Equipment or a targeting computer, is only
 * interchangeable with equipment of the same weight. MASH Equipment bought from the parts store had no weight, as
 * MegaMek only works out its weight on a unit, so no unit would take it.
 */
class EquipmentPartWeightMattersTest {
    private static final String MASH = "MASH Equipment";

    private final Campaign campaign = PartsScenario.create().getCampaign();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static EquipmentType equipmentType(String internalName) {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, internalName);
        return equipmentType;
    }

    /** A spare as the parts store stocks it: no unit, so a unit weight of zero */
    private EquipmentPart storeSpare(String internalName, double size) {
        return new EquipmentPart(0, equipmentType(internalName), -1, size, false, campaign);
    }

    /** A MASH slot on a 60 ton vehicle, as a refit adding one theater lists it */
    private MissingEquipmentPart mashSlotOnASixtyTonVehicle() {
        return new MissingEquipmentPart(60, equipmentType(MASH), 0, campaign, 3.5, 1.0, false);
    }

    private Part saveAndLoad(Part part) throws Exception {
        StringWriter partXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(partXml);
        part.writeToXML(printWriter, 0);
        printWriter.flush();
        Element partElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          partXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
        Part loadedPart = Part.generateInstanceFromXML(partElement, new Version());
        loadedPart.setCampaign(campaign);
        return loadedPart;
    }

    @Test
    void weightMattersForEquipmentWhoseTypeDoesNotFixItsWeight() {
        assertTrue(EquipmentPart.isWeightMatters(equipmentType(MASH)));
        assertTrue(EquipmentPart.isWeightMatters(equipmentType("ISTargeting Computer")));
        assertTrue(EquipmentPart.isWeightMatters(equipmentType("CLTargeting Computer")));
        assertFalse(EquipmentPart.isWeightMatters(equipmentType("Medium Laser")));
        assertFalse(EquipmentPart.isWeightMatters(null));
    }

    @Test
    void storeMashEquipmentWeighsItsCoreComponentAndTheaters() {
        assertEquals(3.5, storeSpare(MASH, 1.0).getTonnage());
        assertEquals(4.5, storeSpare(MASH, 2.0).getTonnage());
    }

    @Test
    void aMashSlotTakesStoreMashEquipment() {
        assertTrue(mashSlotOnASixtyTonVehicle().isAcceptableReplacement(storeSpare(MASH, 1.0), true));
    }

    @Test
    void aMashSlotRefusesMashEquipmentWithMoreTheaters() {
        assertFalse(mashSlotOnASixtyTonVehicle().isAcceptableReplacement(storeSpare(MASH, 2.0), true));
    }

    @Test
    void mashEquipmentSavedWithoutAWeightLoadsWithItsWeight() throws Exception {
        EquipmentPart savedWithoutAWeight = storeSpare(MASH, 1.0);
        savedWithoutAWeight.setEquipTonnage(EquipmentType.TONNAGE_VARIABLE);

        Part loaded = saveAndLoad(savedWithoutAWeight);

        assertInstanceOf(EquipmentPart.class, loaded);
        assertEquals(3.5, loaded.getTonnage());
        assertTrue(mashSlotOnASixtyTonVehicle().isAcceptableReplacement(loaded, true),
              "MASH Equipment bought before this fix is taken by a unit needing it");
    }

    @Test
    void aTargetingComputerSavedWithoutAWeightStaysWithoutOne() throws Exception {
        EquipmentPart savedWithoutAWeight = storeSpare("ISTargeting Computer", 1.0);
        savedWithoutAWeight.setEquipTonnage(EquipmentType.TONNAGE_VARIABLE);

        Part loaded = saveAndLoad(savedWithoutAWeight);

        assertEquals(EquipmentType.TONNAGE_VARIABLE, loaded.getTonnage(),
              "Only a unit can give a targeting computer its weight, so none is made up for it");
    }
}

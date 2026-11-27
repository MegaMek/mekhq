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
package mekhq.campaign.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;

/**
 * MegaMek removed the playtest Armor-Piercing, Precision and Rotary AC Caseless ammo. A campaign whose shopping list
 * still held playtest ammo loaded it with no equipment type and then crashed pricing the shopping list, so saved
 * playtest ammo must load as the standard ammo that replaced it.
 */
class RemovedEquipmentNamesTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @ParameterizedTest
    @CsvSource({
          "IS Armor-Piercing Playtest Ammo AC/5, IS Armor-Piercing Ammo AC/5",
          "IS Armor-Piercing Playtest Ammo AC/10, IS Armor-Piercing Ammo AC/10",
          "IS Precision Playtest Ammo AC/10, IS Precision Ammo AC/10",
          "ISRotaryAC5 Playtest Caseless Ammo, ISRotaryAC5 Caseless Ammo"
    })
    void removedPlaytestAmmoIsReplacedWithStandardAmmo(String playtestName, String standardName) {
        assertNotNull(EquipmentType.get(standardName), standardName + " was not found");

        assertEquals(standardName, RemovedEquipmentNames.replaceRemovedTypeName(playtestName));
    }

    @ParameterizedTest
    @CsvSource({
          "IS Armor-Piercing Ammo AC/5",
          "Not An Equipment Type",
          "Not An Equipment Type Playtest Caseless"
    })
    void namesWithoutRemovedEquipmentAreUnchanged(String typeName) {
        assertEquals(typeName, RemovedEquipmentNames.replaceRemovedTypeName(typeName));
    }

    @Test
    void savedPlaytestAmmoStorageLoadsAsStandardAmmo() throws Exception {
        Element partElement = parse("""
              <part id="0" type="mekhq.campaign.parts.AmmoStorage">
                  <id>0</id>
                  <name>Armor-Piercing Playtest AC/5 Ammo</name>
                  <unitTonnage>1</unitTonnage>
                  <quantity>1</quantity>
                  <typeName>IS Armor-Piercing Playtest Ammo AC/5</typeName>
                  <shots>12</shots>
              </part>""");

        Part part = Part.generateInstanceFromXML(partElement, new Version());

        AmmoStorage ammoStorage = assertInstanceOf(AmmoStorage.class, part);
        assertSame(EquipmentType.get("IS Armor-Piercing Ammo AC/5"), ammoStorage.getType());
        assertEquals("Armor-Piercing AC/5 Ammo", ammoStorage.getName());
    }

    @Test
    void savedEntitySlotsNameStandardAmmo() throws Exception {
        Element entityElement = parse("""
              <entity chassis="Marauder" model="MAD-3R" type="Biped">
                  <location index="3">
                      <slot index="1" type="IS Armor-Piercing Playtest Ammo AC/5" shots="12"/>
                      <slot index="2" type="Autocannon/5"/>
                  </location>
              </entity>""");

        RemovedEquipmentNames.replaceRemovedEquipmentInEntity(entityElement);

        Element ammoSlot = (Element) entityElement.getElementsByTagName("slot").item(0);
        Element weaponSlot = (Element) entityElement.getElementsByTagName("slot").item(1);
        assertEquals("IS Armor-Piercing Ammo AC/5", ammoSlot.getAttribute("type"));
        assertEquals("Autocannon/5", weaponSlot.getAttribute("type"));
    }

    private static Element parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                     .newDocumentBuilder()
                     .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                     .getDocumentElement();
    }
}

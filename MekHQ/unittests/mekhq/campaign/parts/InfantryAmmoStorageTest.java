/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.parts;

import megamek.Version;
import megamek.common.AmmoType;
import megamek.common.EquipmentTypeLookup;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static mekhq.campaign.parts.AmmoUtilities.getInfantryWeapon;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class InfantryAmmoStorageTest {
    @Test
    public void infantryAmmoStorageDeserializationCtorTest() {
        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage();
        assertNotNull(ammoStorage);
    }

    @Test
    public void infantryAmmoStorageCtorTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int shots = weaponType.getShots() * 3;
        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, shots, weaponType, mockCampaign);
        assertEquals(ammoType, ammoStorage.getType());
        assertEquals(weaponType, ammoStorage.getWeaponType());
        assertEquals(shots, ammoStorage.getShots());
    }

    @Test
    public void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int shots = weaponType.getShots() * 3;
        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, shots, weaponType, mockCampaign);

        InfantryAmmoStorage clone = ammoStorage.clone();
        assertEquals(ammoStorage.getType(), clone.getType());
        assertEquals(ammoStorage.getWeaponType(), clone.getWeaponType());
        assertEquals(ammoStorage.getShots(), clone.getShots());
    }

    @Test
    public void getNewPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int shots = ammoType.getShots() * 3;
        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, shots, weaponType, mockCampaign);

        InfantryAmmoStorage newPart = ammoStorage.getNewPart();
        assertEquals(ammoStorage.getType(), newPart.getType());
        assertEquals(ammoStorage.getWeaponType(), newPart.getWeaponType());
        assertEquals(weaponType.getShots(), newPart.getShots());
    }

    @Test
    public void getTechAdvancementTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, 0, weaponType, mockCampaign);

        assertEquals(weaponType.getTechAdvancement(), ammoStorage.getTechAdvancement());
    }

    @Test
    public void infantryAmmoStorageWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        Campaign mockCampaign = mock(Campaign.class);
        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, 7 * ammoType.getShots(), weaponType,
                mockCampaign);
        ammoStorage.setId(25);

        // Write the AmmoStorage XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoStorage.writeToXML(pw, 0);

        // Get the AmmoStorage XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoStorage
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(InfantryAmmoStorage.class, deserializedPart);

        InfantryAmmoStorage deserialized = (InfantryAmmoStorage) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoStorage.getId(), deserialized.getId());
        assertEquals(ammoStorage.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoStorage.getType(), deserialized.getType());
        assertEquals(ammoStorage.getWeaponType(), deserialized.getWeaponType());
        assertEquals(ammoStorage.getShots(), deserialized.getShots());
        assertEquals(ammoStorage.getName(), deserialized.getName());
    }

    @Test
    public void isSameAmmoTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        InfantryWeapon otherWeaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_TAG);

        int shots = ammoType.getShots() * 3;
        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, shots, weaponType, mockCampaign);

        assertTrue(ammoStorage.isSameAmmoType(ammoType, weaponType));
        assertFalse(ammoStorage.isSameAmmoType(ammoType, otherWeaponType));
        assertFalse(ammoStorage.isSameAmmoType(otherAmmoType, weaponType));
        assertFalse(ammoStorage.isSameAmmoType(otherAmmoType, otherWeaponType));
    }

    @Test
    public void isCompatibleAmmoTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int shots = ammoType.getShots() * 3;
        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, shots, weaponType, mockCampaign);

        assertFalse(ammoStorage.isCompatibleAmmo(ammoType));
        assertFalse(ammoStorage.isCompatibleAmmo(otherAmmoType));
    }

    @Test
    public void isSamePartTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        InfantryWeapon otherWeaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_TAG);

        InfantryAmmoStorage ammoStorage = new InfantryAmmoStorage(0, ammoType, 0, weaponType, mockCampaign);

        Part otherPart = new InfantryAmmoStorage(0, ammoType, 0, weaponType, mockCampaign);
        assertTrue(ammoStorage.isSamePartType(otherPart));

        otherPart = new InfantryAmmoStorage(0, otherAmmoType, 0, weaponType, mockCampaign);
        assertFalse(ammoStorage.isSamePartType(otherPart));

        otherPart = new InfantryAmmoStorage(0, ammoType, 0, otherWeaponType, mockCampaign);
        assertFalse(ammoStorage.isSamePartType(otherPart));

        otherPart = new InfantryAmmoStorage(0, otherAmmoType, 0, otherWeaponType, mockCampaign);
        assertFalse(ammoStorage.isSamePartType(otherPart));

        otherPart = new AmmoStorage(0, ammoType, 0, mockCampaign);
        assertFalse(ammoStorage.isSamePartType(otherPart));

        otherPart = new MekLocation();
        assertFalse(ammoStorage.isSamePartType(otherPart));
    }
}

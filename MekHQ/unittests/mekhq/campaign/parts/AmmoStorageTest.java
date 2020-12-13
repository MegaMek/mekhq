/*
 * Copyright (C) 2020 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static mekhq.campaign.parts.AmmoUtilities.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import megamek.common.AmmoType;
import megamek.common.BombType;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.work.IAcquisitionWork;

public class AmmoStorageTest {
    @Test
    public void ammoStorageDeserializationCtorTest() {
        AmmoStorage ammoStorage = new AmmoStorage();
        assertNotNull(ammoStorage);
    }

    @Test
    public void ammoStorageCtorTest() {
        AmmoType ammoType = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, ammoType.getShots(), mockCampaign);

        assertEquals(ammoType, ammoStorage.getType());
        assertEquals(ammoType.getShots(), ammoStorage.getShots());
        assertEquals(1.0, ammoStorage.getTonnage(), 0.001);
    }

    @Test
    public void getMissingPartTest() {
        AmmoType ammoType = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, ammoType.getShots(), mockCampaign);

        // There should be no missing part.
        assertNull(ammoStorage.getMissingPart());
    }

    @Test
    public void cloneTest() {
        AmmoType ammoType = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, 2 * ammoType.getShots(), mockCampaign);

        AmmoStorage clone = ammoStorage.clone();
        assertNotNull(clone);

        assertEquals(ammoStorage.getType(), clone.getType());
        assertEquals(ammoStorage.getBuyCost(), clone.getBuyCost());
        assertEquals(ammoStorage.getCurrentValue(), clone.getCurrentValue());
        assertEquals(ammoStorage.getShots(), clone.getShots());
    }

    @Test
    public void getNewPartTest() {
        AmmoType ammoType = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, 2 * ammoType.getShots(), mockCampaign);

        // Create a new part...
        AmmoStorage newAmmoStorage = ammoStorage.getNewPart();
        assertNotNull(newAmmoStorage);

        // ... and the new part should be identical in ALMOST every way...
        assertEquals(ammoStorage.getType(), newAmmoStorage.getType());
        assertEquals(ammoStorage.getBuyCost(), newAmmoStorage.getBuyCost());

        // ... except for the number of shots, which should be instead
        // equal to the default number of shots for the type.
        assertEquals(ammoType.getShots(), newAmmoStorage.getShots());
    }

    @Test
    public void getNewEquipmentTest() {
        AmmoType ammoType = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, 2 * ammoType.getShots(), mockCampaign);

        // Create a new part...
        AmmoStorage newAmmoStorage = ammoStorage.getNewEquipment();
        assertNotNull(newAmmoStorage);

        // ... and the new part should be identical in ALMOST every way...
        assertEquals(ammoStorage.getType(), newAmmoStorage.getType());
        assertEquals(ammoStorage.getBuyCost(), newAmmoStorage.getBuyCost());

        // ... except for the number of shots, which should be instead
        // equal to the default number of shots for the type.
        assertEquals(ammoType.getShots(), newAmmoStorage.getShots());
    }

    @Test
    public void getAcquisitionWorkTest() {
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, 2 * ammoType.getShots(), mockCampaign);

        // Create a new acquisition work...
        IAcquisitionWork acquisitionWork = ammoStorage.getAcquisitionWork();
        assertNotNull(acquisitionWork);

        // Check getNewEquipment()...
        Object newEquipment = acquisitionWork.getNewEquipment();
        assertNotNull(newEquipment);
        assertTrue(newEquipment instanceof AmmoStorage);

        AmmoStorage newAmmoStorage = (AmmoStorage) newEquipment;

        // ... and the new part should be identical in ALMOST every way...
        assertEquals(ammoStorage.getType(), newAmmoStorage.getType());
        assertEquals(ammoStorage.getBuyCost(), newAmmoStorage.getBuyCost());

        // ... except for the number of shots, which should be instead
        // equal to the default number of shots for the type.
        assertEquals(ammoType.getShots(), newAmmoStorage.getShots());

        // Check getAcquisitionPart()
        Part acquisitionPart = acquisitionWork.getAcquisitionPart();
        assertNotNull(acquisitionPart);
        assertTrue(acquisitionPart instanceof AmmoStorage);

        newAmmoStorage = (AmmoStorage) acquisitionPart;

        // ... and the new part should be identical in ALMOST every way...
        assertEquals(ammoStorage.getType(), newAmmoStorage.getType());
        assertEquals(ammoStorage.getBuyCost(), newAmmoStorage.getBuyCost());

        // ... except for the number of shots, which should be instead
        // equal to the default number of shots for the type.
        assertEquals(ammoType.getShots(), newAmmoStorage.getShots());
    }

    @Test
    public void ammoStorageShotsTest() {
        AmmoStorage ammoStorage = new AmmoStorage();

        // We begin empty...
        assertEquals(0, ammoStorage.getShots());

        // ... and if we add some ammo...
        ammoStorage.changeShots(10);

        // ... we'll then hold that amount.
        assertEquals(10, ammoStorage.getShots());

        // We can also remove ammo...
        ammoStorage.changeShots(-5);
        assertEquals(5, ammoStorage.getShots());

        // ... but if we try to remove more than exists...
        ammoStorage.changeShots(-20);

        // ... we'll never have less than zero.
        assertEquals(0, ammoStorage.getShots());

        // Likewise, if we set the amount of shots...
        ammoStorage.setShots(20);
        assertEquals(20, ammoStorage.getShots());

        // ... we still can't set it to be less than zero.
        ammoStorage.setShots(-20);
        assertEquals(0, ammoStorage.getShots());
    }

    @Test
    public void isSamePartTypeTest() {
        AmmoType ammoType = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, ammoType.getShots(), mockCampaign);

        // We're the same as ourselves.
        assertTrue(ammoStorage.isSamePartType(ammoStorage));

        // We're the same as our clone.
        AmmoStorage clone = ammoStorage.clone();
        assertTrue(ammoStorage.isSamePartType(clone));
        assertTrue(clone.isSamePartType(ammoStorage));

        // We're the same as another ammo storage of the same type
        // but with different constructor values.
        AmmoStorage otherAmmoStorage = new AmmoStorage(1, ammoType, 0, mockCampaign);
        assertTrue(ammoStorage.isSamePartType(otherAmmoStorage));
        assertTrue(otherAmmoStorage.isSamePartType(ammoStorage));

        // We're not the same as some other part.
        assertFalse(ammoStorage.isSamePartType(new MekSensor()));
        assertFalse(ammoStorage.isSamePartType(new AmmoBin()));
        assertFalse(ammoStorage.isSamePartType(new AmmoStorage()));

        // Create an ammo type with some different munitions available
        AmmoType isSRM2Ammo = getAmmoType("ISSRM2 Ammo");
        ammoStorage = new AmmoStorage(0, isSRM2Ammo, isSRM2Ammo.getShots(), mockCampaign);

        // And ensure they're not the same as the same type of ammo, just
        // a different munition type.
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        otherAmmoStorage = new AmmoStorage(0, isSRM2InfernoAmmo, isSRM2InfernoAmmo.getShots(), mockCampaign);
        assertFalse(ammoStorage.isSamePartType(otherAmmoStorage));
        assertFalse(otherAmmoStorage.isSamePartType(ammoStorage));
    }

    @Test
    public void isSamePartTypeBombTest() {
        BombType bombType = getBombType("HEBomb");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, bombType, bombType.getShots(), mockCampaign);

        // We're the same as ourselves.
        assertTrue(ammoStorage.isSamePartType(ammoStorage));

        // We're the same as our clone.
        AmmoStorage clone = ammoStorage.clone();
        assertTrue(ammoStorage.isSamePartType(clone));
        assertTrue(clone.isSamePartType(ammoStorage));

        // We're the same as another ammo storage of the same type
        // but with different constructor values.
        AmmoStorage otherAmmoStorage = new AmmoStorage(1, bombType, 0, mockCampaign);
        assertTrue(ammoStorage.isSamePartType(otherAmmoStorage));
        assertTrue(otherAmmoStorage.isSamePartType(ammoStorage));

        // We're not the same as some other part.
        assertFalse(ammoStorage.isSamePartType(new MekSensor()));
        assertFalse(ammoStorage.isSamePartType(new AmmoBin()));
        assertFalse(ammoStorage.isSamePartType(new AmmoStorage()));

        // Create a bomb ammo type with a different bomb type
        AmmoType infernoBomb = getBombType("InfernoBomb");
        otherAmmoStorage = new AmmoStorage(0, infernoBomb, infernoBomb.getShots(), mockCampaign);

        // And ensure they're not the same as the same type of ammo, just
        // a different munition type.
        assertFalse(ammoStorage.isSamePartType(otherAmmoStorage));
        assertFalse(otherAmmoStorage.isSamePartType(ammoStorage));
    }

    @Test
    public void isSameAmmoTypeTest() {
        AmmoType ammoType = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, ammoType, ammoType.getShots(), mockCampaign);

        // We're the same as ourselves.
        assertTrue(ammoStorage.isSameAmmoType(ammoType));

        // Create an ammo type with some different munitions available
        AmmoType isSRM2Ammo = getAmmoType("ISSRM2 Ammo");
        assertFalse(ammoStorage.isSameAmmoType(isSRM2Ammo));

        ammoStorage = new AmmoStorage(0, isSRM2Ammo, isSRM2Ammo.getShots(), mockCampaign);

        // And ensure they're not the same as the same type of ammo, just
        // a different munition type.
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        assertFalse(ammoStorage.isSameAmmoType(isSRM2InfernoAmmo));
    }

    @Test
    public void isSameAmmoTypeFullHalfTest() {
        Campaign mockCampaign = mock(Campaign.class);

        // Create Full and Half bins
        Map<String, String> fullAndHalfs = new HashMap<>();
        fullAndHalfs.put("IS Ammo MG - Full", "IS Machine Gun Ammo - Half");
        fullAndHalfs.put("Clan Machine Gun Ammo - Full", "Clan Machine Gun Ammo - Half");
        fullAndHalfs.put("IS Light Machine Gun Ammo - Full", "IS Light Machine Gun Ammo - Half");
        fullAndHalfs.put("Clan Light Machine Gun Ammo - Full", "Clan Light Machine Gun Ammo - Half");
        fullAndHalfs.put("IS Heavy Machine Gun Ammo - Full", "IS Heavy Machine Gun Ammo - Half");
        fullAndHalfs.put("Clan Heavy Machine Gun Ammo - Full", "Clan Heavy Machine Gun Ammo - Half");
        fullAndHalfs.put("IS Ammo Nail/Rivet - Full", "IS Ammo Nail/Rivet - Half");
        for (Map.Entry<String, String> pair : fullAndHalfs.entrySet()) {
            AmmoType fullType = getAmmoType(pair.getKey());
            AmmoType halfType = getAmmoType(pair.getValue());

            AmmoStorage full = new AmmoStorage(0, fullType, fullType.getShots(), mockCampaign);
            assertTrue(full.isSameAmmoType(halfType));

            AmmoStorage half = new AmmoStorage(0, halfType, halfType.getShots(), mockCampaign);
            assertTrue(half.isSameAmmoType(fullType));
        }
    }

    @Test
    public void getTonnageTest() {
        AmmoType isAC5Ammo = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, isAC5Ammo, isAC5Ammo.getShots(), mockCampaign);

        // If we have the default number of shots, we should have 1 ton.
        assertEquals(1.0, ammoStorage.getTonnage(), 0.001);

        // Likewise, if we have double the number of shots, we should have 2 tons.
        ammoStorage.setShots(2 * isAC5Ammo.getShots());
        assertEquals(2.0, ammoStorage.getTonnage(), 0.001);

        // And if we have zero shots, we should have zero tons.
        ammoStorage.setShots(0);
        assertEquals(0.0, ammoStorage.getTonnage(), 0.001);
    }

    @Test
    public void getTonnageKgTest() {
        AmmoType mockAmmoType = mock(AmmoType.class);
        double kgPerShot = 0.1;
        when(mockAmmoType.getKgPerShot()).thenReturn(kgPerShot);
        Campaign mockCampaign = mock(Campaign.class);

        int shots = 50;
        AmmoStorage ammoStorage = new AmmoStorage(0, mockAmmoType, shots, mockCampaign);
        assertEquals((shots * kgPerShot) / 1000.0, ammoStorage.getTonnage(), 0.001);
    }

    @Test
    public void getCurrentValueTest() {
        AmmoType isAC5Ammo = getAmmoType("ISAC5 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoStorage ammoStorage = new AmmoStorage(0, isAC5Ammo, 0, mockCampaign);

        // If we have no rounds of ammo, we shouldn't cost anything.
        assertEquals(Money.zero(), ammoStorage.getCurrentValue());

        // And if we have the default quantity...
        ammoStorage.setShots(isAC5Ammo.getShots());

        // ... we should cost the default amount.
        assertEquals(ammoStorage.getBuyCost(), ammoStorage.getCurrentValue());
        assertEquals(ammoStorage.getStickerPrice(), ammoStorage.getCurrentValue());

        // And if we have twice the amount of ammo...
        ammoStorage.setShots(2 * isAC5Ammo.getShots());

        // ... we should cost twice as much.
        assertEquals(ammoStorage.getBuyCost().multipliedBy(2.0), ammoStorage.getCurrentValue());
    }

    @Test
    public void ammoStorageWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        AmmoStorage ammoStorage = new AmmoStorage(0, isSRM2InfernoAmmo, 3 * isSRM2InfernoAmmo.getShots(), mockCampaign);
        ammoStorage.setId(25);

        // Write the AmmoStorage XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoStorage.writeToXml(pw, 0);

        // Get the AmmoStorage XML
        String xml = sw.toString();
        assertFalse(xml.trim().isEmpty());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoStorage
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version("1.0.0"));
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof AmmoStorage);

        AmmoStorage deserialized = (AmmoStorage) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoStorage.getId(), deserialized.getId());
        assertEquals(ammoStorage.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoStorage.getType(), deserialized.getType());
        assertEquals(ammoStorage.getShots(), deserialized.getShots());
    }

    @Test
    public void ammoStorageBombWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        BombType infernoBomb = getBombType("InfernoBomb");
        Campaign mockCampaign = mock(Campaign.class);
        AmmoStorage ammoStorage = new AmmoStorage(0, infernoBomb, 3 * infernoBomb.getShots(), mockCampaign);
        ammoStorage.setId(25);

        // Write the AmmoStorage XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoStorage.writeToXml(pw, 0);

        // Get the AmmoStorage XML
        String xml = sw.toString();
        assertFalse(xml.trim().isEmpty());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoStorage
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version("1.0.0"));
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof AmmoStorage);

        AmmoStorage deserialized = (AmmoStorage) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoStorage.getId(), deserialized.getId());
        assertEquals(ammoStorage.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoStorage.getType(), deserialized.getType());
        assertEquals(ammoStorage.getShots(), deserialized.getShots());
    }
}

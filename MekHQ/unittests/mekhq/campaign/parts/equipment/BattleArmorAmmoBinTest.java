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

package mekhq.campaign.parts.equipment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static mekhq.campaign.parts.AmmoUtilities.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import megamek.common.AmmoType;
import mekhq.MekHqXmlUtil;
import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;

public class BattleArmorAmmoBinTest {
    @Test
    public void deserializationCtorTest() {
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin();
        assertNotNull(ammoBin);
    }

    @Test
    public void canNeverScrapTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots();
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, mockCampaign);

        assertTrue(ammoBin.canNeverScrap());
    }

    @Test
    public void needsMaintenanceTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots();
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, mockCampaign);

        assertFalse(ammoBin.needsMaintenance());
    }

    @Test
    public void battleArmorAmmoBinCtorTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots();
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, mockCampaign);

        assertEquals(ammoType, ammoBin.getType());
        assertEquals(equipmentNum, ammoBin.getEquipmentNum());
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        assertEquals(ammoType.getShots(), ammoBin.getFullShots());
        assertEquals(mockCampaign, ammoBin.getCampaign());
    }

    @Test
    public void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots() - 1;
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, mockCampaign);

        // Ensure the clone has all the same stuff
        BattleArmorAmmoBin clone = ammoBin.clone();
        assertEquals(ammoBin.getType(), clone.getType());
        assertEquals(ammoBin.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(ammoBin.getShotsNeeded(), clone.getShotsNeeded());
        assertEquals(ammoBin.getFullShots(), clone.getFullShots());
        assertEquals(ammoBin.getCampaign(), clone.getCampaign());
        assertEquals(ammoBin.getName(), clone.getName());
    }

    @Test
    public void battleArmorAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, isSRM2InfernoAmmo, 42, isSRM2InfernoAmmo.getShots() - 1, false, mockCampaign);
        ammoBin.setId(25);

        // Write the BattleArmorAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXml(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof BattleArmorAmmoBin);

        BattleArmorAmmoBin deserialized = (BattleArmorAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void oneShotBattleArmorAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, isSRM2InfernoAmmo, 42, 0, true, mockCampaign);
        ammoBin.setId(25);

        // Write the BattleArmorAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXml(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof BattleArmorAmmoBin);

        BattleArmorAmmoBin deserialized = (BattleArmorAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.isOneShot(), deserialized.isOneShot());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void fullBattleArmorAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, isSRM2InfernoAmmo, 42, 0, false, mockCampaign);
        ammoBin.setId(25);

        // Write the BattleArmorAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXml(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof BattleArmorAmmoBin);

        BattleArmorAmmoBin deserialized = (BattleArmorAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void emptyBattleArmorAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, isSRM2InfernoAmmo, 42, isSRM2InfernoAmmo.getShots(), false, mockCampaign);
        ammoBin.setId(25);

        // Write the BattleArmorAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXml(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof BattleArmorAmmoBin);

        BattleArmorAmmoBin deserialized = (BattleArmorAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }
}

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import static mekhq.campaign.parts.AmmoUtilities.*;

import mekhq.campaign.parts.enums.PartRepairType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class MissingAmmoBinTest {
    @Test
    public void deserializationCtorTest() {
        MissingAmmoBin ammoBin = new MissingAmmoBin();
        assertNotNull(ammoBin);
    }

    @Test
    public void missingAmmoBinMassRepairOptionType() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, ammoType, 18, false, false, mockCampaign);

        assertEquals(PartRepairType.AMMO, missingAmmoBin.getMassRepairOptionType());
    }

    @Test
    public void getNewPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, ammoType, 18, false, false, mockCampaign);

        // Get a new part that represents the missing bin
        AmmoBin newPart = missingAmmoBin.getNewPart();
        assertEquals(missingAmmoBin.getType(), newPart.getType());
        assertTrue(newPart.getEquipmentNum() < 0);
        assertEquals(missingAmmoBin.getFullShots(), newPart.getFullShots());
        assertEquals(missingAmmoBin.getCampaign(), newPart.getCampaign());
        assertEquals(missingAmmoBin.getName(), newPart.getName());
        assertEquals(missingAmmoBin.isOneShot(), newPart.isOneShot());
        assertFalse(newPart.isOmniPodded());

        // One-shot, Omnipodded missing ammo bin
        ammoType = getAmmoType("ISSRM6 Ammo");
        missingAmmoBin = new MissingAmmoBin(0, ammoType, 18, true, true, mockCampaign);

        // Get a new part that represents the missing bin
        newPart = missingAmmoBin.getNewPart();
        assertEquals(missingAmmoBin.getType(), newPart.getType());
        assertTrue(newPart.getEquipmentNum() < 0);
        assertEquals(missingAmmoBin.getFullShots(), newPart.getFullShots());
        assertEquals(missingAmmoBin.getCampaign(), newPart.getCampaign());
        assertEquals(missingAmmoBin.getName(), newPart.getName());
        assertEquals(missingAmmoBin.isOneShot(), newPart.isOneShot());
        assertFalse(newPart.isOmniPodded());
    }

    @Test
    public void missingAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, isSRM2InfernoAmmo, 42, false, false, mockCampaign);
        missingAmmoBin.setId(25);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        missingAmmoBin.writeToXml(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version("1.0.0"));
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof MissingAmmoBin);

        MissingAmmoBin deserialized = (MissingAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(missingAmmoBin.getId(), deserialized.getId());
        assertEquals(missingAmmoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(missingAmmoBin.getType(), deserialized.getType());
        assertEquals(missingAmmoBin.getFullShots(), deserialized.getFullShots());
        assertEquals(missingAmmoBin.isOneShot(), deserialized.isOneShot());
        assertEquals(missingAmmoBin.isOmniPodded(), deserialized.isOmniPodded());
        assertEquals(missingAmmoBin.getName(), deserialized.getName());
    }

    @Test
    public void oneShotMissingAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, isSRM2InfernoAmmo, 42, true, true, mockCampaign);
        missingAmmoBin.setId(25);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        missingAmmoBin.writeToXml(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version("1.0.0"));
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof MissingAmmoBin);

        MissingAmmoBin deserialized = (MissingAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(missingAmmoBin.getId(), deserialized.getId());
        assertEquals(missingAmmoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(missingAmmoBin.getType(), deserialized.getType());
        assertEquals(missingAmmoBin.getFullShots(), deserialized.getFullShots());
        assertEquals(missingAmmoBin.isOneShot(), deserialized.isOneShot());
        assertEquals(missingAmmoBin.isOmniPodded(), deserialized.isOmniPodded());
        assertEquals(missingAmmoBin.getName(), deserialized.getName());
    }

    @Test
    public void isAcceptableReplacementSameTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Ammo");

        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, ammoType, 18, false, false, mockCampaign);

        // Same type AmmoBin
        AmmoBin replacementBin = new AmmoBin(0, ammoType, -1, 0, false, false, mockCampaign);

        // Check and see if same type AmmoBin replacement works.
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Use an Ammo with a different munition type
        missingAmmoBin = new MissingAmmoBin(0, otherAmmoType, 18, false, false, mockCampaign);
        replacementBin = new AmmoBin(0, otherAmmoType, -1, 0, false, false, mockCampaign);

        // Check and see if same type AmmoBin replacement works.
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Use a one-shot ammo bin
        missingAmmoBin = new MissingAmmoBin(0, otherAmmoType, 18, true, false, mockCampaign);
        replacementBin = new AmmoBin(0, otherAmmoType, -1, 0, true, false, mockCampaign);

        // Check and see if same type AmmoBin replacement works.
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Use an omni-podded ammo bin
        missingAmmoBin = new MissingAmmoBin(0, otherAmmoType, 18, false, true, mockCampaign);
        replacementBin = new AmmoBin(0, otherAmmoType, -1, 0, false, false, mockCampaign);

        // Check and see if same type AmmoBin replacement works.
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, true));
    }

    @Test
    public void isAcceptableReplacementDifferentTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Ammo");

        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, ammoType, 18, false, false, mockCampaign);

        // Different Ammo Type
        AmmoBin replacementBin = new AmmoBin(0, otherAmmoType, -1, 0, false, false, mockCampaign);

        // Check and see if this replacement fails.
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Same ammo type, different one-shot status
        missingAmmoBin = new MissingAmmoBin(0, ammoType, 18, false, false, mockCampaign);
        replacementBin = new AmmoBin(0, ammoType, -1, 0, true, false, mockCampaign);

        // Check and see if this replacement fails.
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Another different one-shot status
        missingAmmoBin = new MissingAmmoBin(0, ammoType, 18, true, false, mockCampaign);
        replacementBin = new AmmoBin(0, ammoType, -1, 0, false, false, mockCampaign);

        // Check and see if this replacement fails.
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Different AmmoBin type
        InfantryAmmoBin otherAmmoBin = mock(InfantryAmmoBin.class);
        assertFalse(missingAmmoBin.isAcceptableReplacement(otherAmmoBin, false));
        assertFalse(missingAmmoBin.isAcceptableReplacement(otherAmmoBin, true));

        // Different Part type
        MekLocation otherPartType = mock(MekLocation.class);
        assertFalse(missingAmmoBin.isAcceptableReplacement(otherPartType, false));
        assertFalse(missingAmmoBin.isAcceptableReplacement(otherPartType, true));
    }

    @Test
    public void fixFindsAcceptableReplacementTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        // Create a missing ammo bin on a unit
        int equipmentNum = 18;
        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, ammoType, equipmentNum, false, false, mockCampaign);
        Unit unit = mock(Unit.class);
        ArgumentCaptor<Part> replacementCaptor = ArgumentCaptor.forClass(Part.class);
        doAnswer(ans -> {
            Part replacement = ans.getArgument(0);
            replacement.setUnit(unit);
            return null;
        }).when(unit).addPart(replacementCaptor.capture());
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        missingAmmoBin.setUnit(unit);
        quartermaster.addPart(missingAmmoBin, 0);

        // Attempt to fix the missing ammo bin
        missingAmmoBin.fix();

        // 0. missingAmmoBin should be removed from the unit and campaign
        assertTrue(missingAmmoBin.getId() < 0);
        assertFalse(warehouse.getParts().contains(missingAmmoBin));
        assertNull(missingAmmoBin.getUnit());

        // 1. Unit should have received a new replacement
        Part replacementPart = replacementCaptor.getValue();
        assertNotNull(replacementPart);
        assertTrue(replacementPart instanceof AmmoBin);

        // 2. And the replacement should match the missing ammo bin
        AmmoBin replacementAmmoBin = (AmmoBin) replacementPart;
        assertTrue(replacementAmmoBin.getId() > 0);
        assertEquals(unit, replacementAmmoBin.getUnit());
        assertEquals(ammoType, replacementAmmoBin.getType());
        assertEquals(equipmentNum, replacementAmmoBin.getEquipmentNum());
        assertEquals(missingAmmoBin.isOneShot(), replacementAmmoBin.isOneShot());
        assertEquals(missingAmmoBin.getFullShots(), replacementAmmoBin.getShotsNeeded());
    }

    @Test
    public void fixFindsAcceptableOneShotReplacementTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create a missing ammo bin on a unit
        int equipmentNum = 18;
        MissingAmmoBin missingAmmoBin = new MissingAmmoBin(0, ammoType, equipmentNum, true, false, mockCampaign);
        Unit unit = mock(Unit.class);
        ArgumentCaptor<Part> replacementCaptor = ArgumentCaptor.forClass(Part.class);
        doAnswer(ans -> {
            Part replacement = ans.getArgument(0);
            replacement.setUnit(unit);
            return null;
        }).when(unit).addPart(replacementCaptor.capture());
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        missingAmmoBin.setUnit(unit);
        quartermaster.addPart(missingAmmoBin, 0);

        // Attempt to fix the missing ammo bin
        missingAmmoBin.fix();

        // 0. missingAmmoBin should be removed from the unit and campaign
        assertTrue(missingAmmoBin.getId() < 0);
        assertFalse(warehouse.getParts().contains(missingAmmoBin));
        assertNull(missingAmmoBin.getUnit());

        // 1. Unit should have received a new replacement
        Part replacementPart = replacementCaptor.getValue();
        assertNotNull(replacementPart);
        assertTrue(replacementPart instanceof AmmoBin);

        // 2. And the replacement should match the missing ammo bin
        AmmoBin replacementAmmoBin = (AmmoBin) replacementPart;
        assertTrue(replacementAmmoBin.getId() > 0);
        assertEquals(unit, replacementAmmoBin.getUnit());
        assertEquals(ammoType, replacementAmmoBin.getType());
        assertEquals(equipmentNum, replacementAmmoBin.getEquipmentNum());
        assertEquals(missingAmmoBin.isOneShot(), replacementAmmoBin.isOneShot());
        assertEquals(missingAmmoBin.getFullShots(), replacementAmmoBin.getShotsNeeded());
    }
}

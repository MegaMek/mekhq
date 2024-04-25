/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.Version;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.equipment.AmmoMounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AmmoBinTest {
    @Test
    public void deserializationCtorTest() {
        AmmoBin ammoBin = new AmmoBin();
        assertNotNull(ammoBin);
    }

    @Test
    public void ammoBinCtorTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots();
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        assertEquals(ammoType, ammoBin.getType());
        assertEquals(equipmentNum, ammoBin.getEquipmentNum());
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        assertEquals(ammoType.getShots(), ammoBin.getFullShots());
        assertEquals(mockCampaign, ammoBin.getCampaign());
    }

    @Test
    public void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots() - 1;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // Ensure the clone has all the same stuff
        AmmoBin clone = ammoBin.clone();
        assertEquals(ammoBin.getType(), clone.getType());
        assertEquals(ammoBin.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(ammoBin.getShotsNeeded(), clone.getShotsNeeded());
        assertEquals(ammoBin.getFullShots(), clone.getFullShots());
        assertEquals(ammoBin.getCampaign(), clone.getCampaign());
        assertEquals(ammoBin.getName(), clone.getName());
    }

    @Test
    public void needsMaintenanceTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, ammoType.getShots(), false, false, mockCampaign);

        // AmmoBins do not need maintenance, even when empty.
        assertFalse(ammoBin.needsMaintenance());
    }

    @Test
    public void isPriceAdjustedForAmountTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, ammoType.getShots(), false, false, mockCampaign);

        assertTrue(ammoBin.isPriceAdjustedForAmount());
    }

    @Test
    public void mrmsOptionTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, ammoType.getShots(), false, false, mockCampaign);

        assertEquals(PartRepairType.AMMUNITION, ammoBin.getMRMSOptionType());
    }

    @Test
    public void isOmniPoddableTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, ammoType.getShots(), false, false, mockCampaign);

        assertTrue(ammoBin.isOmniPoddable());
    }

    @Test
    public void getTechAdvancementTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, ammoType.getShots(), false, false, mockCampaign);

        assertEquals(ammoType.getTechAdvancement(), ammoBin.getTechAdvancement());
    }

    @Test
    public void getNewPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, 0, false, false, mockCampaign);

        // Ensure the new part has all the same stuff
        AmmoStorage ammoStorage = ammoBin.getNewPart();
        assertEquals(ammoBin.getType(), ammoStorage.getType());
        assertEquals(ammoType.getShots(), ammoStorage.getShots());
        assertEquals(ammoBin.getCampaign(), ammoStorage.getCampaign());
    }

    @Test
    public void getNewEquipmentTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, 0, false, false, mockCampaign);

        // Ensure the new part has all the same stuff
        AmmoStorage ammoStorage = ammoBin.getNewEquipment();
        assertEquals(ammoBin.getType(), ammoStorage.getType());
        assertEquals(ammoType.getShots(), ammoStorage.getShots());
        assertEquals(ammoBin.getCampaign(), ammoStorage.getCampaign());
    }

    @Test
    public void getAcquisitionWorkTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        boolean isOneShot = false;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, 0, isOneShot, false, mockCampaign);

        // Grab the missing part via IAcquisitionWork
        IAcquisitionWork acquisitionPart = ammoBin.getAcquisitionWork();
        assertInstanceOf(AmmoStorage.class, acquisitionPart);

        AmmoStorage ammoStorage = (AmmoStorage) acquisitionPart;
        assertEquals(ammoBin.getType(), ammoStorage.getType());
        assertEquals(ammoType.getShots(), ammoStorage.getShots());
        assertEquals(ammoBin.getCampaign(), ammoStorage.getCampaign());

        isOneShot = true;
        ammoBin = new AmmoBin(0, ammoType, -1, 0, isOneShot, false, mockCampaign);

        // Check that we buy a ton, even if the bin is one shot
        acquisitionPart = ammoBin.getAcquisitionWork();
        assertInstanceOf(AmmoStorage.class, acquisitionPart);

        ammoStorage = (AmmoStorage) acquisitionPart;
        assertEquals(ammoBin.getType(), ammoStorage.getType());
        assertEquals(ammoType.getShots(), ammoStorage.getShots());
        assertEquals(ammoBin.getCampaign(), ammoStorage.getCampaign());
    }

    @Test
    public void getMissingPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        int equipmentNum = 18;
        boolean isOneShot = false;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, isOneShot, false, mockCampaign);

        // Ensure the missing part has all the same stuff
        MissingAmmoBin missingBin = ammoBin.getMissingPart();
        assertEquals(ammoBin.getType(), missingBin.getType());
        assertEquals(ammoBin.getEquipmentNum(), missingBin.getEquipmentNum());
        assertEquals(ammoBin.getFullShots(), missingBin.getFullShots());
        assertEquals(ammoBin.getCampaign(), missingBin.getCampaign());
        assertEquals(ammoBin.getName(), missingBin.getName());
        assertEquals(ammoBin.isOneShot(), missingBin.isOneShot());

        isOneShot = true;
        ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, isOneShot, false, mockCampaign);

        // Ensure the missing part has all the same stuff
        missingBin = ammoBin.getMissingPart();
        assertEquals(ammoBin.getType(), missingBin.getType());
        assertEquals(ammoBin.getEquipmentNum(), missingBin.getEquipmentNum());
        assertEquals(ammoBin.getFullShots(), missingBin.getFullShots());
        assertEquals(ammoBin.getCampaign(), missingBin.getCampaign());
        assertEquals(ammoBin.getName(), missingBin.getName());
        assertEquals(ammoBin.isOneShot(), missingBin.isOneShot());
    }

    @Test
    public void setShotsNeeded() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISAC10 Ammo");

        // Create an Ammo Bin with some ammo ...
        int shotsNeeded = 1;
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        doAnswer(invocation -> {
            // Update the ammo type returned by mounted
            AmmoType newAmmoType = invocation.getArgument(0);
            when(mockMounted.getType()).thenReturn(newAmmoType);
            return null;
        }).when(mockMounted).changeAmmoType(any());
        doAnswer(invocation -> {
            // Update the shots left when we're updated
            int shotsLeft = invocation.getArgument(0);
            when(mockMounted.getBaseShotsLeft()).thenReturn(shotsLeft);
            return null;
        }).when(mockMounted).setShotsLeft(anyInt());

        // Ensure the ammo bin starts with the shots we asked for.
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        assertTrue(ammoBin.needsFixing());

        // Set the number of shots needed ...
        ammoBin.setShotsNeeded(ammoType.getShots());

        // ... and ensure we get the correct count back.
        assertEquals(ammoType.getShots(), ammoBin.getShotsNeeded());
        assertTrue(ammoBin.needsFixing());

        // Ensure we never need negative shots.
        ammoBin.setShotsNeeded(-1);
        assertEquals(0, ammoBin.getShotsNeeded());
        assertFalse(ammoBin.needsFixing());
    }

    @Test
    public void getFullShotsUsesAmmoTypeShotsIfNoEntityOrMounted() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISAC10 Ammo");

        // Create an ammo bin without a unit (?) or entity or valid mount ...
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, 0, false, false, mockCampaign);

        // ... and ensure it reports the shots from the ammo type
        assertEquals(ammoType.getShots(), ammoBin.getFullShots());
    }

    @Test
    public void getFullShotsOneShotAmmoReturnsOneShot() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISAC10 Ammo");

        // Create a One Shot ammo bin ...
        boolean isOneShot = true;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, 0, isOneShot, false, mockCampaign);

        // ... and ensure it only reports a single shot
        assertEquals(1, ammoBin.getFullShots());
    }

    @Test
    public void getFullShotsUsesOriginalShotsFromMounted() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISAC10 Ammo");

        // Create an ammo bin with a given ammo type ...
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        int originalShots = 32;
        when(mockMounted.getOriginalShots()).thenReturn(originalShots);

        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and ensure it reports the shots from the mounted and not the ammo type
        assertEquals(originalShots, ammoBin.getFullShots());
    }

    @Test
    public void getFullShotsForProtomechsReducedInHalfForNonStandardMunitions() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an ammobin with a non-standard munition type ...
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, false, false, mockCampaign);

        // ... place the ammo bin on a ProtoMech unit ...
        Unit mockUnit = mock(Unit.class);
        Protomech mockEntity = mock(Protomech.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        int originalShots = 32;
        when(mockMounted.getOriginalShots()).thenReturn(originalShots);

        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and ensure it reports the shots from the ammo type
        assertEquals(originalShots / 2, ammoBin.getFullShots());
    }

    @Test
    public void getBaseTimeSalvagingTest() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Not an omnipodded ammo bin ...
        boolean isOmniPodded = false;
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, false, isOmniPodded, mockCampaign);
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(true);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(mounted.isOmniPodMounted()).thenReturn(isOmniPodded);
        when(entity.getEquipment(eq(equipmentNum))).thenReturn(mounted);
        ammoBin.setUnit(unit);

        // Salvage of a normal ammo bin is 120 minutes
        assertEquals(120, ammoBin.getBaseTime());

        // An omnipodded ammo bin ...
        isOmniPodded = true;
        ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, false, isOmniPodded, mockCampaign);
        when(mounted.isOmniPodMounted()).thenReturn(isOmniPodded);
        ammoBin.setUnit(unit);

        // Salvage of an omni ammo bin is 30 minutes
        assertEquals(30, ammoBin.getBaseTime());
    }

    @Test
    public void getBaseTimeRepairTest() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // An ammo bin whose ammo type matches the mount ...
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, false, false, mockCampaign);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(eq(equipmentNum))).thenReturn(mounted);
        ammoBin.setUnit(unit);

        // Repair of a normal ammo bin is 15 minutes if the ammo types match
        assertEquals(15, ammoBin.getBaseTime());

        AmmoType otherAmmoType = getAmmoType("ISSRM6 Ammo");

        // An ammo bin whose ammo type does NOT match the mount ...
        ammoBin = new AmmoBin(0, ammoType, equipmentNum, 0, false, false, mockCampaign);
        when(mounted.getType()).thenReturn(otherAmmoType);
        ammoBin.setUnit(unit);

        // Repair of a bin with different ammo types is 30 minutes
        assertEquals(30, ammoBin.getBaseTime());
    }

    @Test
    public void ammoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        AmmoBin ammoBin = new AmmoBin(0, isSRM2InfernoAmmo, 42,
                isSRM2InfernoAmmo.getShots() - 1, false, false, mockCampaign);
        ammoBin.setId(25);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(AmmoBin.class, deserializedPart);

        AmmoBin deserialized = (AmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void oneShotAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        AmmoBin ammoBin = new AmmoBin(0, isSRM2InfernoAmmo, 42, 0, true, false, mockCampaign);
        ammoBin.setId(25);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(AmmoBin.class, deserializedPart);

        AmmoBin deserialized = (AmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.isOneShot(), deserialized.isOneShot());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void fullAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        AmmoBin ammoBin = new AmmoBin(0, isSRM2InfernoAmmo, 42, 0, false, false, mockCampaign);
        ammoBin.setId(25);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(AmmoBin.class, deserializedPart);

        AmmoBin deserialized = (AmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void emptyAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        AmmoBin ammoBin = new AmmoBin(0, isSRM2InfernoAmmo, 42, isSRM2InfernoAmmo.getShots(), false, false, mockCampaign);
        ammoBin.setId(25);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(AmmoBin.class, deserializedPart);

        AmmoBin deserialized = (AmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void changeMunitionTest() {
        AmmoType isSRM2Ammo = getAmmoType("ISSRM2 Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        int equipmentNum = 19;
        AmmoBin ammoBin = new AmmoBin(0, isSRM2Ammo, equipmentNum, 0, false, false, mockCampaign);

        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(isSRM2Ammo);
        when(mockMounted.getBaseShotsLeft()).thenReturn(isSRM2Ammo.getShots());
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        doAnswer(invocation -> {
            // Update the ammo type returned by mounted
            AmmoType newAmmoType = invocation.getArgument(0);
            when(mockMounted.getType()).thenReturn(newAmmoType);
            return null;
        }).when(mockMounted).changeAmmoType(any());
        doAnswer(invocation -> {
            // Update the shots left when we're updated
            int shotsLeft = invocation.getArgument(0);
            when(mockMounted.getBaseShotsLeft()).thenReturn(shotsLeft);
            return null;
        }).when(mockMounted).setShotsLeft(anyInt());
        ammoBin.setUnit(mockUnit);

        // Before we do anything there should be nothing to fix on a full bin.
        assertFalse(ammoBin.needsFixing());

        // Pick a different munition type
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        ammoBin.changeMunition(isSRM2InfernoAmmo);

        assertEquals(isSRM2InfernoAmmo, ammoBin.getType());
        assertTrue(ammoBin.needsFixing());

        assertEquals(isSRM2InfernoAmmo.getShots(), ammoBin.getShotsNeeded());
    }

    @Test
    public void changeMunitionSerializationTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);

        AmmoBin ammoBin = new AmmoBin(0, isSRM2InfernoAmmo, -1, 0, false, false, mockCampaign);

        // Pick a different munition type
        AmmoType isSRM2Ammo = getAmmoType("ISSRM2 Ammo");
        ammoBin.changeMunition(isSRM2Ammo);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(AmmoBin.class, deserializedPart);

        AmmoBin deserialized = (AmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void unloadEmptyBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an empty Ammo Bin...
        int shotsNeeded = ammoType.getShots();
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and unload it.
        ammoBin.unload();

        // Nothing should be added to the Warehouse.
        assertTrue(warehouse.getParts().isEmpty());
    }

    @Test
    public void unloadFullBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create a full Ammo Bin...
        int shotsNeeded = 0;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and unload it.
        ammoBin.unload();

        // We should now have a 1 ton of ammo in our warehouse
        AmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(AmmoStorage.class, part);
            added = (AmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(ammoType.getShots(), added.getShots());
    }

    @Test
    public void unloadPartialBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with just one round...
        int shotsNeeded = ammoType.getShots() - 1;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and unload it.
        ammoBin.unload();

        // We should now have that ammo in our warehouse.
        AmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(AmmoStorage.class, part);
            added = (AmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(1, added.getShots());
    }

    @Test
    public void salvageEmptyBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an empty Ammo Bin...
        int shotsNeeded = ammoType.getShots();
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and salvage it.
        ammoBin.remove(true);

        // Nothing should be added to the Warehouse.
        assertTrue(warehouse.getParts().isEmpty());
        assertEquals(0, ammoBin.getAmountAvailable());
    }

    @Test
    public void salvageFullBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create a full Ammo Bin...
        int shotsNeeded = 0;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and salvage it.
        ammoBin.remove(true);

        // We should now have a 1 ton of ammo in our warehouse
        AmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(AmmoStorage.class, part);
            added = (AmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(ammoType.getShots(), added.getShots());
        assertEquals(ammoType.getShots(), ammoBin.getAmountAvailable());
    }

    @Test
    public void salvagePartialBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with just one round...
        int shotsNeeded = ammoType.getShots() - 1;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and salvage it.
        ammoBin.remove(true);

        // We should now have that ammo in our warehouse.
        AmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(AmmoStorage.class, part);
            added = (AmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(1, added.getShots());
        assertEquals(1, ammoBin.getAmountAvailable());
    }

    @Test
    public void loadBinWithoutUnitDoesNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with no ammo...
        int shotsNeeded = ammoType.getShots();
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and try to load it when the warehouse is empty.
        ammoBin.loadBin();

        // We should have not changed how many shots are needed
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
    }

    @Test
    public void loadBinWithoutSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and try to load it when the warehouse is empty.
        ammoBin.loadBin();

        // We should have not changed how many shots are needed
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));
    }

    @Test
    public void loadBinWithOnlySpareAmmoOfWrongType() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add ammo of the wrong type to the warehouse ...
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Inferno Ammo");
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots());

        // ... and try to load it.
        ammoBin.loadBin();

        // We should have not changed how many shots are needed ...
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));

        // ... nor how many shots are available of the wrong type.
        assertEquals(otherAmmoType.getShots(), quartermaster.getAmmoAvailable(otherAmmoType));
    }

    @Test
    public void loadBinWithJustEnoughSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the right type to the warehouse ...
        quartermaster.addAmmo(ammoType, ammoType.getShots());

        // ... and try to load it.
        ammoBin.loadBin();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and no more ammo available in the warehouse
        assertEquals(0, quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void loadBinWithMoreThanEnoughSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        // Create an Ammo Bin with plenty of ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add more than enough ammo of the right type to the warehouse ...
        int shotsOnHand = 10 * ammoType.getShots();
        quartermaster.addAmmo(ammoType, shotsOnHand);

        // ... and try to load it.
        ammoBin.loadBin();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and only the ammo needed was pulled from the warehouse
        assertEquals(shotsOnHand - shotsNeeded, quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void loadEmptyBinAfterChangingAmmoType() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        doAnswer(invocation -> {
            // Update the ammo type returned by mounted
            AmmoType newAmmoType = invocation.getArgument(0);
            when(mockMounted.getType()).thenReturn(newAmmoType);
            return null;
        }).when(mockMounted).changeAmmoType(any());
        doAnswer(invocation -> {
            // Update the shots left when we're updated
            int shotsLeft = invocation.getArgument(0);
            when(mockMounted.getBaseShotsLeft()).thenReturn(shotsLeft);
            return null;
        }).when(mockMounted).setShotsLeft(anyInt());

        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of both types to the warehouse ...
        quartermaster.addAmmo(ammoType, ammoType.getShots());
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots());

        // ... then change the munition type of the ammo bin ...
        ammoBin.changeMunition(otherAmmoType);

        // ... and try to load it.
        ammoBin.loadBin();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).changeAmmoType(eq(otherAmmoType));
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and no more of the new ammo available in the warehouse.
        assertEquals(0, quartermaster.getAmmoAvailable(otherAmmoType));

        // ... but the correct amount of our original ammo type.
        assertEquals(ammoType.getShots(), quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void loadFullBinAfterChangingAmmoType() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin full of ammo ...
        int shotsNeeded = 0;
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(ammoType.getShots());
        doAnswer(invocation -> {
            // Update the ammo type returned by mounted
            AmmoType newAmmoType = invocation.getArgument(0);
            when(mockMounted.getType()).thenReturn(newAmmoType);
            return null;
        }).when(mockMounted).changeAmmoType(any());
        doAnswer(invocation -> {
            // Update the shots left when we're updated
            int shotsLeft = invocation.getArgument(0);
            when(mockMounted.getBaseShotsLeft()).thenReturn(shotsLeft);
            return null;
        }).when(mockMounted).setShotsLeft(anyInt());

        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the new type to the warehouse ...
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots());

        // ... then change the munition type of the ammo bin ...
        ammoBin.changeMunition(otherAmmoType);

        // ... and try to load it.
        ammoBin.loadBin();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).changeAmmoType(eq(otherAmmoType));
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and no more of the new ammo available in the warehouse.
        assertEquals(0, quartermaster.getAmmoAvailable(otherAmmoType));

        // ... but the correct amount of our original ammo type unloaded from the bin.
        assertEquals(ammoType.getShots(), quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void fixBinWithoutUnitDoesNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with no ammo...
        int shotsNeeded = ammoType.getShots();
        AmmoBin ammoBin = new AmmoBin(0, ammoType, -1, shotsNeeded, false, false, mockCampaign);

        // ...and try to load it when the warehouse is empty.
        ammoBin.fix();

        // We should have not changed how many shots are needed
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
    }

    @Test
    public void fixBinWithoutSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and try to load it when the warehouse is empty.
        ammoBin.fix();

        // We should have not changed how many shots are needed
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));
    }

    @Test
    public void fixBinWithOnlySpareAmmoOfWrongType() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add ammo of the wrong type to the warehouse ...
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Inferno Ammo");
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots());

        // ... and try to load it.
        ammoBin.fix();

        // We should have not changed how many shots are needed ...
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));

        // ... nor how many shots are available of the wrong type.
        assertEquals(otherAmmoType.getShots(), quartermaster.getAmmoAvailable(otherAmmoType));
    }

    @Test
    public void fixBinWithJustEnoughSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the right type to the warehouse ...
        quartermaster.addAmmo(ammoType, ammoType.getShots());

        // ... and try to load it.
        ammoBin.fix();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and no more ammo available in the warehouse
        assertEquals(0, quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void fixBinWithMoreThanEnoughSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        // Create an Ammo Bin with plenty of ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add more than enough ammo of the right type to the warehouse ...
        int shotsOnHand = 10 * ammoType.getShots();
        quartermaster.addAmmo(ammoType, shotsOnHand);

        // ... and try to load it.
        ammoBin.fix();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and only the ammo needed was pulled from the warehouse
        assertEquals(shotsOnHand - shotsNeeded, quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void fixEmptyBinAfterChangingAmmoType() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin with no ammo ...
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        doAnswer(invocation -> {
            // Update the ammo type returned by mounted
            AmmoType newAmmoType = invocation.getArgument(0);
            when(mockMounted.getType()).thenReturn(newAmmoType);
            return null;
        }).when(mockMounted).changeAmmoType(any());
        doAnswer(invocation -> {
            // Update the shots left when we're updated
            int shotsLeft = invocation.getArgument(0);
            when(mockMounted.getBaseShotsLeft()).thenReturn(shotsLeft);
            return null;
        }).when(mockMounted).setShotsLeft(anyInt());

        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of both types to the warehouse ...
        quartermaster.addAmmo(ammoType, ammoType.getShots());
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots());

        // ... then change the munition type of the ammo bin ...
        ammoBin.changeMunition(otherAmmoType);

        // ... and try to load it.
        ammoBin.fix();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).changeAmmoType(eq(otherAmmoType));
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and no more of the new ammo available in the warehouse.
        assertEquals(0, quartermaster.getAmmoAvailable(otherAmmoType));

        // ... but the correct amount of our original ammo type.
        assertEquals(ammoType.getShots(), quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void fixFullBinAfterChangingAmmoType() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create an Ammo Bin full of ammo ...
        int shotsNeeded = 0;
        int equipmentNum = 42;
        AmmoBin ammoBin = new AmmoBin(0, ammoType, equipmentNum, shotsNeeded, false, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(ammoType.getShots());
        doAnswer(invocation -> {
            // Update the ammo type returned by mounted
            AmmoType newAmmoType = invocation.getArgument(0);
            when(mockMounted.getType()).thenReturn(newAmmoType);
            return null;
        }).when(mockMounted).changeAmmoType(any());
        doAnswer(invocation -> {
            // Update the shots left when we're updated
            int shotsLeft = invocation.getArgument(0);
            when(mockMounted.getBaseShotsLeft()).thenReturn(shotsLeft);
            return null;
        }).when(mockMounted).setShotsLeft(anyInt());

        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the new type to the warehouse ...
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots());

        // ... then change the munition type of the ammo bin ...
        ammoBin.changeMunition(otherAmmoType);

        // ... and try to load it.
        ammoBin.fix();

        // We should have no shots needed ...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).changeAmmoType(eq(otherAmmoType));
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and no more of the new ammo available in the warehouse.
        assertEquals(0, quartermaster.getAmmoAvailable(otherAmmoType));

        // ... but the correct amount of our original ammo type unloaded from the bin.
        assertEquals(ammoType.getShots(), quartermaster.getAmmoAvailable(ammoType));
    }
}

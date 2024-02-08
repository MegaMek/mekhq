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
import megamek.common.*;
import megamek.common.equipment.AmmoMounted;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.InfantryAmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
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
import java.util.Arrays;

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static mekhq.campaign.parts.AmmoUtilities.getInfantryWeapon;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InfantryAmmoBinTest {
    @Test
    public void deserializationCtorTest() {
        InfantryAmmoBin ammoBin = new InfantryAmmoBin();
        assertNotNull(ammoBin);
    }

    @Test
    public void ammoBinCtorTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int equipmentNum = 18;
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        assertEquals(ammoType, ammoBin.getType());
        assertEquals(weaponType, ammoBin.getWeaponType());
        assertEquals(equipmentNum, ammoBin.getEquipmentNum());
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        assertEquals(weaponType.getShots() * clips, ammoBin.getFullShots());
        assertEquals(mockCampaign, ammoBin.getCampaign());
    }

    @Test
    public void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int equipmentNum = 18;
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * (clips - 1);
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // Ensure the clone has all the same stuff
        InfantryAmmoBin clone = ammoBin.clone();
        assertEquals(ammoBin.getType(), clone.getType());
        assertEquals(ammoBin.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(ammoBin.getShotsNeeded(), clone.getShotsNeeded());
        assertEquals(ammoBin.getFullShots(), clone.getFullShots());
        assertEquals(ammoBin.getCampaign(), clone.getCampaign());
        assertEquals(ammoBin.getName(), clone.getName());
    }

    @Test
    public void getMissingPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int equipmentNum = 18;
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, 0, weaponType, clips, false, mockCampaign);

        // Ensure the clone has all the same stuff
        MissingInfantryAmmoBin missingBin = ammoBin.getMissingPart();
        assertEquals(ammoBin.getType(), missingBin.getType());
        assertEquals(ammoBin.getWeaponType(), missingBin.getWeaponType());
        assertEquals(ammoBin.getClips(), missingBin.getClips());
        assertEquals(ammoBin.getEquipmentNum(), missingBin.getEquipmentNum());
        assertEquals(ammoBin.getFullShots(), missingBin.getFullShots());
        assertEquals(ammoBin.getCampaign(), missingBin.getCampaign());
        assertEquals(ammoBin.getName(), missingBin.getName());
    }

    @Test
    public void setShotsNeeded() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with some ammo ...
        int clips = 5;
        int shotsNeeded = 1;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted )mockMounted);
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
        ammoBin.setUnit(mockUnit);

        // Ensure the ammo bin starts with the shots we asked for.
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());

        // Set the number of shots needed ...
        ammoBin.setShotsNeeded(weaponType.getShots() * clips);

        // ... and ensure we get the correct count back.
        assertEquals(weaponType.getShots() * clips, ammoBin.getShotsNeeded());

        // Ensure we allow negative shots (used by changeCapacity).
        ammoBin.setShotsNeeded(-1);
        assertEquals(-1, ammoBin.getShotsNeeded());
    }

    @Test
    public void getFullShotsUsesWeaponTypeShots() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an ammo bin without a unit (?) or entity or valid mount ...
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, 0, weaponType, clips, false, mockCampaign);

        // ... and ensure it reports the shots from the ammo type
        assertEquals(weaponType.getShots() * clips, ammoBin.getFullShots());
    }

    @Test
    public void getBaseTimeSalvagingTest() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Not an omnipodded ammo bin ...
        boolean isOmniPodded = false;
        int equipmentNum = 42;
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, 0, weaponType, clips,  isOmniPodded, mockCampaign);
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
        ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, 0, weaponType, clips, isOmniPodded, mockCampaign);
        when(mounted.isOmniPodMounted()).thenReturn(isOmniPodded);
        ammoBin.setUnit(unit);

        // Salvage of an omni ammo bin is 30 minutes
        assertEquals(30, ammoBin.getBaseTime());
    }

    @Test
    public void getBaseTimeRepairTest() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // An ammo bin whose ammo type matches the mount ...
        int equipmentNum = 42;
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, 0, weaponType, clips, false, mockCampaign);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(eq(equipmentNum))).thenReturn(mounted);
        ammoBin.setUnit(unit);

        // Repair of a normal ammo bin is 15 minutes if the ammo types match
        assertEquals(15, ammoBin.getBaseTime());

        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);

        // An ammo bin whose ammo type does NOT match the mount ...
        ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, 0, weaponType, clips, false, mockCampaign);
        when(mounted.getType()).thenReturn(otherAmmoType);
        ammoBin.setUnit(unit);

        // Repair of a bin with different ammo types is 30 minutes
        assertEquals(30, ammoBin.getBaseTime());
    }

    @Test
    public void ammoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType infernoAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        Campaign mockCampaign = mock(Campaign.class);
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, infernoAmmoType, 42, weaponType.getShots() - (clips - 1), weaponType, clips, false, mockCampaign);
        ammoBin.setId(25);

        // Write the InfantryAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the InfantryAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the InfantryAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(InfantryAmmoBin.class, deserializedPart);

        InfantryAmmoBin deserialized = (InfantryAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void oneShotAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType infernoAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        Campaign mockCampaign = mock(Campaign.class);
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, infernoAmmoType, 42, 0, weaponType, clips, false, mockCampaign);
        ammoBin.setId(25);

        // Write the InfantryAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the InfantryAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the InfantryAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(InfantryAmmoBin.class, deserializedPart);

        InfantryAmmoBin deserialized = (InfantryAmmoBin) deserializedPart;

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
        AmmoType infernoAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        Campaign mockCampaign = mock(Campaign.class);
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, infernoAmmoType, 42, 0, weaponType, clips, false, mockCampaign);
        ammoBin.setId(25);

        // Write the InfantryAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the InfantryAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the InfantryAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(InfantryAmmoBin.class, deserializedPart);

        InfantryAmmoBin deserialized = (InfantryAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void emptyAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType infernoAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        Campaign mockCampaign = mock(Campaign.class);
        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, infernoAmmoType, 42, infernoAmmoType.getShots(), weaponType, clips, false, mockCampaign);
        ammoBin.setId(25);

        // Write the InfantryAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the InfantryAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the InfantryAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(InfantryAmmoBin.class, deserializedPart);

        InfantryAmmoBin deserialized = (InfantryAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void changeMunitionTest() {
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        Campaign mockCampaign = mock(Campaign.class);

        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, 0, weaponType, clips, false, mockCampaign);

        // Pick a different munition type
        AmmoType infernoAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        ammoBin.changeMunition(infernoAmmoType);

        assertEquals(infernoAmmoType, ammoBin.getType());

        assertEquals(infernoAmmoType.getShots() * clips, ammoBin.getShotsNeeded());
    }

    @Test
    public void changeMunitionSerializationTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType infernoAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        Campaign mockCampaign = mock(Campaign.class);

        int clips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, infernoAmmoType, -1, 0, weaponType, clips, false, mockCampaign);

        // Pick a different munition type
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        ammoBin.changeMunition(ammoType);

        // Write the InfantryAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the InfantryAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the InfantryAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(InfantryAmmoBin.class, deserializedPart);

        InfantryAmmoBin deserialized = (InfantryAmmoBin) deserializedPart;

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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an empty Ammo Bin...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ...and unload it.
        ammoBin.unload();

        // Nothing should be added to the Warehouse.
        assertTrue(warehouse.getParts().isEmpty());
        assertEquals(0, quartermaster.getAmmoAvailable(ammoType, weaponType));
    }

    @Test
    public void unloadFullBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create a full Ammo Bin...
        int clips = 5;
        int shotsNeeded = 0;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ...and unload it.
        ammoBin.unload();

        // We should now have a 1 ton of ammo in our warehouse
        InfantryAmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(InfantryAmmoStorage.class, part);
            added = (InfantryAmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(weaponType.getShots() * clips, added.getShots());
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(ammoType, weaponType));
    }

    @Test
    public void unloadPartialBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with just one round...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * (clips - 1);
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ...and unload it.
        ammoBin.unload();

        // We should now have that ammo in our warehouse.
        InfantryAmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(InfantryAmmoStorage.class, part);
            added = (InfantryAmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(weaponType.getShots(), added.getShots()); // Just one clip
        assertEquals(weaponType.getShots(), quartermaster.getAmmoAvailable(ammoType, weaponType));
        assertEquals(weaponType.getShots(), ammoBin.getAmountAvailable());
    }

    @Test
    public void salvageEmptyBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an empty Ammo Bin...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ...and salvage it.
        ammoBin.remove(true);

        // Nothing should be added to the Warehouse.
        assertTrue(warehouse.getParts().isEmpty());
        assertEquals(0, quartermaster.getAmmoAvailable(ammoType, weaponType));
        assertEquals(0, ammoBin.getAmountAvailable());
    }

    @Test
    public void salvageFullBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create a full Ammo Bin...
        int clips = 5;
        int shotsNeeded = 0;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ...and salvage it.
        ammoBin.remove(true);

        // We should now have a 1 ton of ammo in our warehouse
        InfantryAmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(InfantryAmmoStorage.class, part);
            added = (InfantryAmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(weaponType.getShots() * clips, added.getShots());
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(ammoType, weaponType));
        assertEquals(weaponType.getShots() * clips, ammoBin.getAmountAvailable());
    }

    @Test
    public void salvagePartialBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with just one clip...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * (clips - 1);
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ...and salvage it.
        ammoBin.remove(true);

        // We should now have that ammo in our warehouse.
        InfantryAmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(InfantryAmmoStorage.class, part);
            added = (InfantryAmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(weaponType.getShots(), added.getShots()); // Just one clip.
        assertEquals(weaponType.getShots(), quartermaster.getAmmoAvailable(ammoType, weaponType));
    }

    @Test
    public void loadBinWithoutUnitDoesNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add ammo of the wrong type to the warehouse ...
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        quartermaster.addAmmo(otherAmmoType, weaponType, weaponType.getShots() * clips);

        // ... and try to load it.
        ammoBin.loadBin();

        // We should have not changed how many shots are needed ...
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));

        // ... nor how many shots are available of the wrong type.
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(otherAmmoType));
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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the right type to the warehouse ...
        quartermaster.addAmmo(ammoType, weaponType, weaponType.getShots() * clips);

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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with plenty of ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add more than enough ammo of the right type to the warehouse ...
        int shotsOnHand = 10 * clips * weaponType.getShots();
        quartermaster.addAmmo(ammoType, weaponType, shotsOnHand);

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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getOriginalShots()).thenReturn(weaponType.getShots() * clips);
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
        quartermaster.addAmmo(ammoType, weaponType, weaponType.getShots() * clips);
        quartermaster.addAmmo(otherAmmoType, weaponType, weaponType.getShots() * clips);

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
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(ammoType));
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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin full of ammo ...
        int clips = 5;
        int shotsNeeded = 0;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getOriginalShots()).thenReturn(weaponType.getShots() * clips);
        when(mockMounted.getBaseShotsLeft()).thenReturn(weaponType.getShots() * clips);
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
        quartermaster.addAmmo(otherAmmoType, weaponType, weaponType.getShots() * clips);

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
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void fixBinWithoutUnitDoesNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, shotsNeeded, weaponType, clips, false, mockCampaign);

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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add ammo of the wrong type to the warehouse ...
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        quartermaster.addAmmo(otherAmmoType, weaponType, weaponType.getShots() * clips);

        // ... and try to load it.
        ammoBin.fix();

        // We should have not changed how many shots are needed ...
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));

        // ... nor how many shots are available of the wrong type.
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(otherAmmoType));
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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the right type to the warehouse ...
        quartermaster.addAmmo(ammoType, weaponType, weaponType.getShots() * clips);

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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with plenty of ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add more than enough ammo of the right type to the warehouse ...
        int shotsOnHand = 10 * weaponType.getShots() * clips;
        quartermaster.addAmmo(ammoType, weaponType, shotsOnHand);

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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin with no ammo ...
        int clips = 5;
        int shotsNeeded = weaponType.getShots() * clips;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
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
        quartermaster.addAmmo(ammoType, weaponType, weaponType.getShots() * clips);
        quartermaster.addAmmo(otherAmmoType, weaponType, weaponType.getShots() * clips);

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
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(ammoType));
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

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin full of ammo ...
        int clips = 5;
        int shotsNeeded = 0;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Infantry mockEntity = mock(Infantry.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        AmmoMounted mockMounted = mock(AmmoMounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getOriginalShots()).thenReturn(weaponType.getShots() * clips);
        when(mockMounted.getBaseShotsLeft()).thenReturn(weaponType.getShots() * clips);
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
        quartermaster.addAmmo(otherAmmoType, weaponType, weaponType.getShots() * clips);

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
        assertEquals(weaponType.getShots() * clips, quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void isSamePartTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType otherAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        InfantryWeapon otherWeaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_TAG);

        int clips = 5;
        int shotsNeeded = 0;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, shotsNeeded, weaponType, clips, false, mockCampaign);

        // Same ammo type, weapon type, and clips
        InfantryAmmoBin otherAmmoBin = new InfantryAmmoBin(0, ammoType, -1, ammoType.getShots(), weaponType, clips, false, mockCampaign);

        assertTrue(ammoBin.isSamePartType(otherAmmoBin));
        assertTrue(otherAmmoBin.isSamePartType(ammoBin));

        // Different ammo type, same weapon type and clips
        otherAmmoBin = new InfantryAmmoBin(0, otherAmmoType, -1, otherAmmoType.getShots(), weaponType, clips, false, mockCampaign);

        assertFalse(ammoBin.isSamePartType(otherAmmoBin));
        assertFalse(otherAmmoBin.isSamePartType(ammoBin));

        // Same ammo type and clips, different weapon type
        otherAmmoBin = new InfantryAmmoBin(0, ammoType, -1, 0, otherWeaponType, clips, false, mockCampaign);

        assertFalse(ammoBin.isSamePartType(otherAmmoBin));
        assertFalse(otherAmmoBin.isSamePartType(ammoBin));

        // Same ammo type and weapon type, different clips
        otherAmmoBin = new InfantryAmmoBin(0, ammoType, -1, ammoType.getShots(), weaponType, clips + 1, false, mockCampaign);

        assertFalse(ammoBin.isSamePartType(otherAmmoBin));
        assertFalse(otherAmmoBin.isSamePartType(ammoBin));

        // Different ammo type and weapon types, same clips
        otherAmmoBin = new InfantryAmmoBin(0, otherAmmoType, -1, 0, otherWeaponType, clips, false, mockCampaign);

        assertFalse(ammoBin.isSamePartType(otherAmmoBin));
        assertFalse(otherAmmoBin.isSamePartType(ammoBin));

        // Different ammo type, weapon types, and clips
        otherAmmoBin = new InfantryAmmoBin(0, otherAmmoType, -1, 0, otherWeaponType, clips + 1, false, mockCampaign);

        assertFalse(ammoBin.isSamePartType(otherAmmoBin));
        assertFalse(otherAmmoBin.isSamePartType(ammoBin));
    }

    @Test
    public void changeCapacityTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        int originalClips = 5;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, -1, 0, weaponType, originalClips, false, mockCampaign);

        assertEquals(0, ammoBin.getShotsNeeded());
        assertEquals(originalClips, ammoBin.getClips());

        // Decrease capacity
        int clips = 4;
        ammoBin.changeCapacity(clips);

        assertEquals(-weaponType.getShots(), ammoBin.getShotsNeeded());
        assertEquals(clips, ammoBin.getClips());

        originalClips = 5;
        ammoBin = new InfantryAmmoBin(0, ammoType, -1, 0, weaponType, originalClips, false, mockCampaign);

        // Increase capacity
        clips = 10;
        ammoBin.changeCapacity(clips);

        assertEquals(weaponType.getShots() * (clips - originalClips), ammoBin.getShotsNeeded());
        assertEquals(clips, ammoBin.getClips());
    }

    @Test
    public void findPartnerBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType(EquipmentTypeLookup.INFANTRY_AMMO);
        AmmoType infernoAmmoType = getAmmoType(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        InfantryWeapon weaponType = getInfantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Create an Ammo Bin to handle standard ammo ...
        int clips = 5;
        int equipmentNum = 42;
        InfantryAmmoBin ammoBin = new InfantryAmmoBin(0, ammoType, equipmentNum, 0, weaponType, clips, false, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        SupportTank mockEntity = mock(SupportTank.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        // Create a Mounted for the standard ammo bin ...
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockEntity.getEquipmentNum(eq(mockMounted))).thenReturn(equipmentNum);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // Create an Inferno Ammo Bin ...
        int infernoClips = 3;
        int infernoEquipmentNum = 12;
        InfantryAmmoBin infernoAmmoBin = new InfantryAmmoBin(0, infernoAmmoType, infernoEquipmentNum, 0, weaponType, infernoClips, false, mockCampaign);

        // Create a Mounted for the Inferno ammo bin ...
        Mounted mockInfernoMounted = mock(Mounted.class);
        when(mockInfernoMounted.getType()).thenReturn(infernoAmmoType);
        when(mockInfernoMounted.getLinkedBy()).thenReturn(mockMounted);
        when(mockMounted.getLinked()).thenReturn(mockInfernoMounted);
        when(mockEntity.getEquipmentNum(eq(mockInfernoMounted))).thenReturn(infernoEquipmentNum);
        when(mockEntity.getEquipment(eq(infernoEquipmentNum))).thenReturn(mockInfernoMounted);
        infernoAmmoBin.setUnit(mockUnit);

        // Create an AmmoBin which clearly isn't correct
        int incorrectEquipmentNum = 18;
        InfantryAmmoBin incorrectBin = new InfantryAmmoBin(0, ammoType, incorrectEquipmentNum, 0, weaponType, 1, false, mockCampaign);
        Mounted mockIncorrectMounted = mock(Mounted.class);
        when(mockIncorrectMounted.getType()).thenReturn(ammoType);
        when(mockEntity.getEquipmentNum(eq(mockIncorrectMounted))).thenReturn(incorrectEquipmentNum);
        when(mockEntity.getEquipment(eq(incorrectEquipmentNum))).thenReturn(mockIncorrectMounted);
        incorrectBin.setUnit(mockUnit);

        // Ensure the unit returns these parts
        when(mockUnit.getParts()).thenReturn(Arrays.asList(new Part[] {
            incorrectBin, ammoBin, infernoAmmoBin,
        }));

        // Check that findPartnerBin pulls the correct bins ...
        assertEquals(infernoAmmoBin, ammoBin.findPartnerBin());
        assertEquals(ammoBin, infernoAmmoBin.findPartnerBin());
        assertNull(incorrectBin.findPartnerBin());

        // Create an ammo bin not on a unit ...
        incorrectBin = new InfantryAmmoBin(0, ammoType, incorrectEquipmentNum, 0, weaponType, 1, false, mockCampaign);

        // ... which should return null.
        assertNull(incorrectBin.findPartnerBin());
    }
}

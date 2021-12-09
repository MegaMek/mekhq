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
import static mekhq.campaign.parts.equipment.EquipmentUtilities.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import megamek.common.Aero;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.EquipmentTypeLookup;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import mekhq.MekHqXmlUtil;
import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class MissingEquipmentPartTest {
    @Test
    public void deserializationCtorTest() {
        MissingEquipmentPart missingPart = new MissingEquipmentPart();
        assertNotNull(missingPart);
    }

    @Test
    public void equipmentPartCtorTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int tonnage = 75;
        double size = 5.0;
        double equipTonnage = 3.0;
        int equipmentNum = 7;
        boolean isOmniPodded = false;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), eq(size));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(tonnage, type, equipmentNum, mockCampaign, equipTonnage, size, isOmniPodded);

        assertEquals(tonnage, missingPart.getUnitTonnage());
        assertEquals(type, missingPart.getType());
        assertEquals(equipmentNum, missingPart.getEquipmentNum());
        assertEquals(size, missingPart.getSize(), 0.001);
        assertEquals(isOmniPodded, missingPart.isOmniPodded());
        assertEquals(equipTonnage, missingPart.getTonnage(), 0.001);
        assertEquals(mockCampaign, missingPart.getCampaign());

        isOmniPodded = true;
        missingPart = new MissingEquipmentPart(tonnage, type, equipmentNum, mockCampaign, equipTonnage, size, isOmniPodded);

        assertEquals(tonnage, missingPart.getUnitTonnage());
        assertEquals(type, missingPart.getType());
        assertEquals(equipmentNum, missingPart.getEquipmentNum());
        assertEquals(size, missingPart.getSize(), 0.001);
        assertEquals(isOmniPodded, missingPart.isOmniPodded());
        assertEquals(equipTonnage, missingPart.getTonnage(), 0.001);
        assertEquals(mockCampaign, missingPart.getCampaign());
    }

    @Test
    public void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int tonnage = 75;
        double size = 5.0;
        double equipTonnage = 3.0;
        int equipmentNum = 7;
        boolean isOmniPodded = false;
        EquipmentType type = mock(EquipmentType.class);

        MissingEquipmentPart missingPart = new MissingEquipmentPart(tonnage, type, equipmentNum, mockCampaign, equipTonnage, size, isOmniPodded);

        MissingEquipmentPart clone = missingPart.clone();

        assertEquals(missingPart.getUnitTonnage(), clone.getUnitTonnage());
        assertEquals(missingPart.getType(), clone.getType());
        assertEquals(missingPart.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(missingPart.getSize(), clone.getSize(), 0.001);
        assertEquals(missingPart.getTonnage(), clone.getTonnage(), 0.001);
        assertEquals(missingPart.isOmniPodded(), clone.isOmniPodded());
        assertEquals(missingPart.getCampaign(), clone.getCampaign());

        isOmniPodded = true;
        missingPart = new MissingEquipmentPart(tonnage, type, equipmentNum, mockCampaign, equipTonnage, size, isOmniPodded);

        clone = missingPart.clone();

        assertEquals(missingPart.getUnitTonnage(), clone.getUnitTonnage());
        assertEquals(missingPart.getType(), clone.getType());
        assertEquals(missingPart.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(missingPart.getSize(), clone.getSize(), 0.001);
        assertEquals(missingPart.getTonnage(), clone.getTonnage(), 0.001);
        assertEquals(missingPart.isOmniPodded(), clone.isOmniPodded());
        assertEquals(missingPart.getCampaign(), clone.getCampaign());
    }

    @Test
    public void getNewPartTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int tonnage = 75;
        double size = 5.0;
        double equipTonnage = 3.0;
        int equipmentNum = 7;
        boolean isOmniPodded = false;
        EquipmentType type = mock(EquipmentType.class);

        MissingEquipmentPart missingPart = new MissingEquipmentPart(tonnage, type, equipmentNum, mockCampaign, equipTonnage, size, isOmniPodded);

        EquipmentPart equipmentPart = missingPart.getNewPart();
        assertNotNull(equipmentPart);

        assertEquals(missingPart.getUnitTonnage(), equipmentPart.getUnitTonnage());
        assertEquals(missingPart.getType(), equipmentPart.getType());
        assertTrue(equipmentPart.getEquipmentNum() < 0);
        assertEquals(missingPart.getSize(), equipmentPart.getSize(), 0.001);
        assertEquals(missingPart.getTonnage(), equipmentPart.getTonnage(), 0.001);
        assertEquals(missingPart.isOmniPodded(), equipmentPart.isOmniPodded());
        assertEquals(missingPart.getCampaign(), equipmentPart.getCampaign());

        isOmniPodded = true;
        missingPart = new MissingEquipmentPart(tonnage, type, equipmentNum, mockCampaign, equipTonnage, size, isOmniPodded);

        equipmentPart = missingPart.getNewPart();
        assertNotNull(missingPart);

        assertEquals(missingPart.getUnitTonnage(), equipmentPart.getUnitTonnage());
        assertEquals(missingPart.getType(), equipmentPart.getType());
        assertTrue(equipmentPart.getEquipmentNum() < 0);
        assertEquals(missingPart.getSize(), equipmentPart.getSize(), 0.001);
        assertEquals(missingPart.getTonnage(), equipmentPart.getTonnage(), 0.001);
        assertEquals(missingPart.isOmniPodded(), equipmentPart.isOmniPodded());
        assertEquals(missingPart.getCampaign(), equipmentPart.getCampaign());
    }

    @Test
    public void isPartForEquipmentTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);

        int equipmentNum = 42;
        int location = Aero.LOC_NOSE;
        Mounted mounted = mock(Mounted.class);
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);
        missingPart.setUnit(unit);

        assertTrue(missingPart.isPartForEquipmentNum(equipmentNum, location));
        assertFalse(missingPart.isPartForEquipmentNum(equipmentNum, Aero.LOC_RWING));
        assertFalse(missingPart.isPartForEquipmentNum(equipmentNum - 1, location));
    }

    @Test
    public void isOmniPoddableTest() {
        Campaign mockCampaign = mock(Campaign.class);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);

        // Not MiscType or WeaponType
        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, 16, mockCampaign, equipTonnage, size, false);
        assertTrue(missingPart.isOmniPoddable());

        // If fixed only, then we're not omnipoddable
        when(type.isOmniFixedOnly()).thenReturn(true);
        assertFalse(missingPart.isOmniPoddable());

        // MiscType
        MiscType miscType = mock(MiscType.class);
        doReturn(1.0).when(miscType).getTonnage(any(), anyDouble());
        missingPart = new MissingEquipmentPart(75, miscType, 16, mockCampaign, equipTonnage, size, false);

        // Just because we're MiscType doesn't mean we're omnipoddable ...
        assertFalse(missingPart.isOmniPoddable());

        // ... we need to be Mech Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return MiscType.F_MECH_EQUIPMENT.equals(flag);
        }).when(miscType).hasFlag(any());
        assertTrue(missingPart.isOmniPoddable());

        // ... or Tank Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return MiscType.F_TANK_EQUIPMENT.equals(flag);
        }).when(miscType).hasFlag(any());
        assertTrue(missingPart.isOmniPoddable());

        // ... or Aero Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return MiscType.F_FIGHTER_EQUIPMENT.equals(flag);
        }).when(miscType).hasFlag(any());
        assertTrue(missingPart.isOmniPoddable());

        // WeaponType
        WeaponType weaponType = mock(WeaponType.class);
        doReturn(1.0).when(weaponType).getTonnage(any(), anyDouble());
        missingPart = new MissingEquipmentPart(75, weaponType, 16, mockCampaign, equipTonnage, size, false);

        // Just because we're WeaponType doesn't mean we're omnipoddable ...
        assertFalse(missingPart.isOmniPoddable());

        // ... we need to be Mech Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_MECH_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        assertTrue(missingPart.isOmniPoddable());

        // ... or Tank Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_TANK_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        assertTrue(missingPart.isOmniPoddable());

        // ... or Fighter Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_AERO_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        assertTrue(missingPart.isOmniPoddable());

        // ... but not Capital scale.
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_AERO_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        when(weaponType.isCapital()).thenReturn(true);
        assertFalse(missingPart.isOmniPoddable());
    }

    @Test
    public void setUnitUpdatesEquipmentTonnage() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        double unitEquipTonnage = 1.0;
        doReturn(unitEquipTonnage).when(type).getTonnage(any(), anyDouble());

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, 6, mockCampaign, equipTonnage, size, false);

        missingPart.setUnit(unit);

        // Ensure we update the equipment tonnage for variable sized equipment
        verify(type, times(1)).getTonnage(eq(entity), eq(size));

        assertEquals(unitEquipTonnage, missingPart.getTonnage(), 0.001);
    }

    @Test
    public void getLocationTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // No unit
        assertEquals(Entity.LOC_NONE, missingPart.getLocation());

        // Assign to a unit
        missingPart.setUnit(unit);

        // No equipment at the equipment num
        assertEquals(Entity.LOC_NONE, missingPart.getLocation());

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_RT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Our location should match up
        assertEquals(location, missingPart.getLocation());
    }

    @Test
    public void getLocationNameTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // No unit
        assertNull(missingPart.getLocationName());

        // Assign to a unit
        missingPart.setUnit(unit);

        // No equipment at the equipment num
        assertNull(missingPart.getLocationName());

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        String locationName = "Mech Right Torso";
        int location = Mech.LOC_RT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(locationName).when(entity).getLocationName(eq(location));

        // Our location should match up
        assertEquals(locationName, missingPart.getLocationName());

        // The mount has no named location
        when(mounted.getLocation()).thenReturn(Entity.LOC_NONE);
        assertNull(missingPart.getLocationName());
    }

    @Test
    public void isInLocationTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        String locationName = "Mech Right Torso";

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // No unit
        assertFalse(missingPart.isInLocation(locationName));

        // Assign to a unit
        missingPart.setUnit(unit);

        // No equipment at the equipment num
        assertFalse(missingPart.isInLocation(locationName));

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_RT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        doReturn(location).when(entity).getLocationFromAbbr(eq(locationName));

        // Our location should match up
        assertTrue(missingPart.isInLocation(locationName));

        // The mount has no named location
        when(mounted.getLocation()).thenReturn(Entity.LOC_NONE);
        assertFalse(missingPart.isInLocation(locationName));

        // Split the mount and have the second location be the one we want
        when(mounted.getLocation()).thenReturn(Mech.LOC_RLEG);
        when(mounted.isSplit()).thenReturn(true);
        when(mounted.getSecondLocation()).thenReturn(location);

        assertTrue(missingPart.isInLocation(locationName));
    }

    @Test
    public void isRearFacingTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // No unit
        assertFalse(missingPart.isRearFacing());

        // Assign to a unit
        missingPart.setUnit(unit);

        // No equipment at the equipment num
        assertFalse(missingPart.isRearFacing());

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        when(mounted.isRearMounted()).thenReturn(true);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Our facing should match up
        assertTrue(missingPart.isRearFacing());

        when(mounted.isRearMounted()).thenReturn(false);

        // Our facing should match up
        assertFalse(missingPart.isRearFacing());
    }

    @Test
    public void equipmentPartWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        EquipmentType type = getEquipmentType(EquipmentTypeLookup.JUMP_JET);
        Campaign mockCampaign = mock(Campaign.class);
        MissingEquipmentPart missingPart = new MissingEquipmentPart(65, type, 42, mockCampaign, 14.0, 18.0, false);
        missingPart.setId(25);

        // Write the MissingEquipmentPart XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        missingPart.writeToXml(pw, 0);

        // Get the MissingEquipmentPart XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the MissingEquipmentPart
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof MissingEquipmentPart);

        MissingEquipmentPart deserialized = (MissingEquipmentPart) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(missingPart.getId(), deserialized.getId());
        assertEquals(missingPart.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(missingPart.getType(), deserialized.getType());
        assertEquals(missingPart.getName(), deserialized.getName());
        assertEquals(missingPart.getSize(), deserialized.getSize(), 0.001);
        assertEquals(missingPart.getTonnage(), deserialized.getTonnage(), 0.001);
    }

    @Test
    public void removeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(null);
            return null;
        }).when(unit).removePart(any());

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);
        missingPart.setId(25);
        missingPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(missingPart);

        // Remove the part (not salvage)
        missingPart.remove(false);

        assertTrue(missingPart.getId() < 0);
        assertNull(missingPart.getUnit());
        assertTrue(warehouse.getParts().isEmpty());

        verify(unit, times(1)).removePart(eq(missingPart));
    }

    @Test
    public void salvageTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(null);
            return null;
        }).when(unit).removePart(any());

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);
        missingPart.setId(25);
        missingPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(missingPart);

        // Salvage the part ... (does nothing but remove the part)
        missingPart.remove(true);

        assertTrue(missingPart.getId() < 0);
        assertNull(missingPart.getUnit());
        assertTrue(warehouse.getParts().isEmpty());

        verify(unit, times(1)).removePart(eq(missingPart));
    }

    @Test
    public void needsFixingTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, 6, mockCampaign, equipTonnage, size, false);

        // Not on a unit
        assertFalse(missingPart.needsFixing());

        missingPart.setUnit(unit);

        // Unit is not repairable
        assertFalse(missingPart.needsFixing());

        when(unit.isRepairable()).thenReturn(true);

        // Unit is repairable
        assertTrue(missingPart.needsFixing());

        when(unit.isSalvage()).thenReturn(true);

        // On a unit being salvaged, we need a tech
        assertFalse(missingPart.needsFixing());

        missingPart.setTech(mock(Person.class));

        // Salvaging with a tech
        assertTrue(missingPart.needsFixing());
    }

    @Test
    public void onBadHipOrShoulderTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // Not on unit
        assertFalse(missingPart.onBadHipOrShoulder());

        // No equipment mounted at that index
        missingPart.setUnit(unit);
        assertFalse(missingPart.onBadHipOrShoulder());

        // Mount equipment at the index
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_LARM;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Just because we've got the correct mount, doesn't mean we're
        // on a bad hip or shoulder
        assertFalse(missingPart.onBadHipOrShoulder());

        // Bust the shoulder/hip
        doReturn(true).when(unit).hasBadHipOrShoulder(eq(location));
        assertTrue(missingPart.onBadHipOrShoulder());

        // Swap over to the secondary location
        doReturn(false).when(unit).hasBadHipOrShoulder(eq(location));
        int secondLocation = Mech.LOC_LT;
        when(mounted.getSecondLocation()).thenReturn(secondLocation);
        when(mounted.isSplit()).thenReturn(true);
        doReturn(true).when(unit).hasBadHipOrShoulder(eq(secondLocation));

        // Still busted
        assertTrue(missingPart.onBadHipOrShoulder());

        // But wait, fixed again
        doReturn(false).when(unit).hasBadHipOrShoulder(eq(location));
        doReturn(false).when(unit).hasBadHipOrShoulder(eq(secondLocation));
        assertFalse(missingPart.onBadHipOrShoulder());
    }

    @Test
    public void checkFixableTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // Not on unit
        assertNull(missingPart.checkFixable());

        // No equipment mounted at that index
        missingPart.setUnit(unit);
        assertNull(missingPart.checkFixable());

        // Salvaging
        when(unit.isSalvage()).thenReturn(true);
        assertNull(missingPart.checkFixable());

        // Turn off salvaging
        when(unit.isSalvage()).thenReturn(false);

        // Mount equipment at the index
        Mounted mounted = mock(Mounted.class);
        String locationName = "Mech Left Torso";
        int location = Mech.LOC_LT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(locationName).when(entity).getLocationName(eq(location));

        // Nothing wrong with the mount
        assertNull(missingPart.checkFixable());

        // Location breached
        doReturn(true).when(unit).isLocationBreached(eq(location));
        doReturn(false).when(unit).isLocationDestroyed(eq(location));
        assertNotNull(missingPart.checkFixable());

        // Location destroyed
        doReturn(false).when(unit).isLocationBreached(eq(location));
        doReturn(true).when(unit).isLocationDestroyed(eq(location));
        assertNotNull(missingPart.checkFixable());

        String secondaryLocationName = "Mech Left Arm";
        int secondaryLocation = Mech.LOC_LARM;
        when(mounted.getSecondLocation()).thenReturn(secondaryLocation);
        when(mounted.isSplit()).thenReturn(true);
        doReturn(secondaryLocationName).when(entity).getLocationName(secondaryLocation);

        // Restore the first location
        doReturn(false).when(unit).isLocationBreached(eq(location));
        doReturn(false).when(unit).isLocationDestroyed(eq(location));

        // Secondary Location breached
        doReturn(true).when(unit).isLocationBreached(eq(secondaryLocation));
        doReturn(false).when(unit).isLocationDestroyed(eq(secondaryLocation));
        assertNotNull(missingPart.checkFixable());

        // Location destroyed
        doReturn(false).when(unit).isLocationBreached(eq(secondaryLocation));
        doReturn(true).when(unit).isLocationDestroyed(eq(secondaryLocation));
        assertNotNull(missingPart.checkFixable());

        // Restore both locations
        doReturn(false).when(unit).isLocationBreached(eq(location));
        doReturn(false).when(unit).isLocationDestroyed(eq(location));
        doReturn(false).when(unit).isLocationBreached(eq(secondaryLocation));
        doReturn(false).when(unit).isLocationDestroyed(eq(secondaryLocation));

        assertNull(missingPart.checkFixable());
    }

    @Test
    public void fixWithoutSparePartsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(null);
            return null;
        }).when(unit).removePart(any());

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);
        missingPart.setId(25);
        missingPart.setUnit(unit);

        warehouse.addPart(missingPart);

        // Try fixing the part without a spare
        missingPart.fix();

        assertTrue(warehouse.getParts().contains(missingPart));
        assertEquals(unit, missingPart.getUnit());
    }

    @Test
    public void fixWithOneSparePartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(null);
            return null;
        }).when(unit).removePart(any());

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);
        missingPart.setId(25);
        missingPart.setUnit(unit);

        EquipmentPart sparePart = missingPart.getNewPart();
        sparePart.setId(21);

        warehouse.addPart(sparePart);
        warehouse.addPart(missingPart);

        // Try fixing the part with a spare
        missingPart.fix();

        assertFalse(warehouse.getParts().contains(missingPart));
        assertTrue(missingPart.getId() < 0);
        assertNull(missingPart.getUnit());

        // Ensure we used up the spare part
        assertFalse(warehouse.getParts().contains(sparePart));
        assertTrue(sparePart.getId() < 0);

        ArgumentCaptor<Part> replacementCaptor = ArgumentCaptor.forClass(Part.class);
        verify(unit, times(1)).addPart(replacementCaptor.capture());

        Part replacement = replacementCaptor.getValue();
        assertTrue(replacement instanceof EquipmentPart);

        EquipmentPart replacementEquipmentPart = (EquipmentPart) replacement;
        assertTrue(replacementEquipmentPart.getId() > 0);
        assertEquals(equipmentNum, replacementEquipmentPart.getEquipmentNum());
        assertEquals(unit, replacementEquipmentPart.getUnit());
        assertTrue(warehouse.getParts().contains(replacementEquipmentPart));

        verify(mounted, times(1)).setMissing(eq(false));
        verify(mounted, times(1)).setHit(eq(false));
        verify(mounted, times(1)).setDestroyed(eq(false));
        verify(mounted, times(1)).setRepairable(eq(true));
        verify(unit, times(1)).repairSystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum));
    }

    @Test
    public void fixWithManySparePartsTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(null);
            return null;
        }).when(unit).removePart(any());

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);
        missingPart.setId(25);
        missingPart.setUnit(unit);

        int onHand = 27;
        EquipmentPart sparePart = missingPart.getNewPart();
        sparePart.setId(21);
        sparePart.setQuantity(onHand);

        warehouse.addPart(sparePart);
        warehouse.addPart(missingPart);

        // Try fixing the part with a spare
        missingPart.fix();

        assertFalse(warehouse.getParts().contains(missingPart));
        assertTrue(missingPart.getId() < 0);
        assertNull(missingPart.getUnit());

        // Ensure we used only one of the spare parts
        assertTrue(warehouse.getParts().contains(sparePart));
        assertEquals(onHand - 1, sparePart.getQuantity());

        ArgumentCaptor<Part> replacementCaptor = ArgumentCaptor.forClass(Part.class);
        verify(unit, times(1)).addPart(replacementCaptor.capture());

        Part replacement = replacementCaptor.getValue();
        assertTrue(replacement instanceof EquipmentPart);

        EquipmentPart replacementEquipmentPart = (EquipmentPart) replacement;
        assertTrue(replacementEquipmentPart.getId() > 0);
        assertEquals(equipmentNum, replacementEquipmentPart.getEquipmentNum());
        assertEquals(unit, replacementEquipmentPart.getUnit());
        assertTrue(warehouse.getParts().contains(replacementEquipmentPart));

        verify(mounted, times(1)).setMissing(eq(false));
        verify(mounted, times(1)).setHit(eq(false));
        verify(mounted, times(1)).setDestroyed(eq(false));
        verify(mounted, times(1)).setRepairable(eq(true));
        verify(unit, times(1)).repairSystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum));
    }

    @Test
    public void updateConditionFromPartTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // No unit? This is a no-op
        missingPart.updateConditionFromPart();

        missingPart.setUnit(unit);

        // No equipment mounted at equipmentNum? This is a no-op
        missingPart.updateConditionFromPart();

        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Equipment mounted at the location
        missingPart.updateConditionFromPart();

        verify(mounted, times(1)).setHit(eq(true));
        verify(mounted, times(1)).setDestroyed(eq(true));
        verify(mounted, times(1)).setRepairable(eq(false));
        verify(unit, times(1)).destroySystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum));
    }

    @Test
    public void getBaseTimeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, 42, mockCampaign, equipTonnage, size, false);
        missingPart.setUnit(unit);

        // Missing parts are 120 minutes ...
        assertEquals(120, missingPart.getBaseTime());

        // ... except when omni-podded.
        missingPart.setOmniPodded(true);
        assertEquals(30, missingPart.getBaseTime());
    }

    @Test
    public void getDifficultyTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double equipTonnage = 2.0;
        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), anyDouble());

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, 42, mockCampaign, equipTonnage, size, false);
        missingPart.setUnit(unit);

        // Missing parts are +0
        assertEquals(0, missingPart.getDifficulty());
    }

    @Test
    public void isAcceptableReplacementTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        int equipmentNum = 42;
        double equipTonnage = 2.0;
        double size = 3.0;
        double cost = 12.0;
        MiscType type = mock(MiscType.class);
        when(type.getRawCost()).thenReturn(cost);
        doReturn(equipTonnage).when(type).getTonnage(any(), eq(size));

        MissingEquipmentPart missingPart = new MissingEquipmentPart(75, type, equipmentNum, mockCampaign, equipTonnage, size, false);

        // We can't replace ourselves with ourselves
        assertFalse(missingPart.isAcceptableReplacement(missingPart, false));

        // We can't replace ourselves with another missing part
        Part otherPart = missingPart.clone();
        assertFalse(missingPart.isAcceptableReplacement(otherPart, false));

        // We're the same even if unit tonnage differs, as long as our
        // equipment tonnage is the same.
        otherPart = new EquipmentPart(65, type, 42, size, false, mockCampaign);
        assertTrue(missingPart.isAcceptableReplacement(otherPart, false));

        // We're not the same if types differ
        MiscType otherType = mock(MiscType.class);
        otherPart = new EquipmentPart(75, otherType, 42, size, false, mockCampaign);
        assertFalse(missingPart.isAcceptableReplacement(otherPart, false));

        // We're not the same if sizes differ
        double otherSize = 2.0;
        doReturn(1.75).when(type).getTonnage(any(), eq(otherSize));
        otherPart = new EquipmentPart(75, type, 42, otherSize, false, mockCampaign);
        assertFalse(missingPart.isAcceptableReplacement(otherPart, false));

        // We're not the same if one is omni-podded and the other isn't
        otherPart = new EquipmentPart(75, type, 42, size, true, mockCampaign);
        assertFalse(missingPart.isAcceptableReplacement(otherPart, false));

        // We're not the same if our sticker prices differ;

        // Setup a type with variable costs
        doReturn(true).when(type).hasFlag(eq(MiscType.F_OFF_ROAD));
        doReturn((double) EquipmentType.COST_VARIABLE).when(type).getRawCost();

        // Put the variable cost part back on a unit
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_CT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(cost * 10.0).when(type).getCost(eq(entity), anyBoolean(), eq(location), eq(size));

        // And now the other part is not on a unit, so no location hence different cost
        otherPart = new EquipmentPart(75, type, 42, size, false, mockCampaign);
        assertTrue(missingPart.isAcceptableReplacement(otherPart, false));
    }
}

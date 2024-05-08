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
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.bayweapons.BayWeapon;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static mekhq.campaign.parts.equipment.EquipmentUtilities.getEquipmentType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EquipmentPartTest {
    @Test
    public void deserializationCtorTest() {
        EquipmentPart equipmentPart = new EquipmentPart();
        assertNotNull(equipmentPart);
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

        EquipmentPart equipmentPart = new EquipmentPart(tonnage, type, equipmentNum, size, isOmniPodded, mockCampaign);

        assertEquals(tonnage, equipmentPart.getUnitTonnage());
        assertEquals(type, equipmentPart.getType());
        assertEquals(equipmentNum, equipmentPart.getEquipmentNum());
        assertEquals(size, equipmentPart.getSize(), 0.001);
        assertEquals(isOmniPodded, equipmentPart.isOmniPodded());
        assertEquals(equipTonnage, equipmentPart.getTonnage(), 0.001);
        assertEquals(mockCampaign, equipmentPart.getCampaign());

        isOmniPodded = true;
        equipmentPart = new EquipmentPart(tonnage, type, equipmentNum, size, isOmniPodded, mockCampaign);

        assertEquals(tonnage, equipmentPart.getUnitTonnage());
        assertEquals(type, equipmentPart.getType());
        assertEquals(equipmentNum, equipmentPart.getEquipmentNum());
        assertEquals(size, equipmentPart.getSize(), 0.001);
        assertEquals(isOmniPodded, equipmentPart.isOmniPodded());
        assertEquals(equipTonnage, equipmentPart.getTonnage(), 0.001);
        assertEquals(mockCampaign, equipmentPart.getCampaign());
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
        doReturn(equipTonnage).when(type).getTonnage(any(), eq(size));

        EquipmentPart equipmentPart = new EquipmentPart(tonnage, type, equipmentNum, size, isOmniPodded, mockCampaign);

        EquipmentPart clone = equipmentPart.clone();

        assertEquals(equipmentPart.getUnitTonnage(), clone.getUnitTonnage());
        assertEquals(equipmentPart.getType(), clone.getType());
        assertEquals(equipmentPart.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(equipmentPart.getSize(), clone.getSize(), 0.001);
        assertEquals(equipmentPart.getTonnage(), clone.getTonnage(), 0.001);
        assertEquals(equipmentPart.isOmniPodded(), clone.isOmniPodded());
        assertEquals(equipmentPart.getCampaign(), clone.getCampaign());

        isOmniPodded = true;
        equipmentPart = new EquipmentPart(tonnage, type, equipmentNum, size, isOmniPodded, mockCampaign);

        clone = equipmentPart.clone();

        assertEquals(equipmentPart.getUnitTonnage(), clone.getUnitTonnage());
        assertEquals(equipmentPart.getType(), clone.getType());
        assertEquals(equipmentPart.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(equipmentPart.getSize(), clone.getSize(), 0.001);
        assertEquals(equipmentPart.getTonnage(), clone.getTonnage(), 0.001);
        assertEquals(equipmentPart.isOmniPodded(), clone.isOmniPodded());
        assertEquals(equipmentPart.getCampaign(), clone.getCampaign());
    }

    @Test
    public void getMissingPartTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int tonnage = 75;
        double size = 5.0;
        double equipTonnage = 3.0;
        int equipmentNum = 7;
        boolean isOmniPodded = false;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(equipTonnage).when(type).getTonnage(any(), eq(size));

        EquipmentPart equipmentPart = new EquipmentPart(tonnage, type, equipmentNum, size, isOmniPodded, mockCampaign);

        MissingEquipmentPart missingPart = equipmentPart.getMissingPart();
        assertNotNull(missingPart);

        assertEquals(equipmentPart.getUnitTonnage(), missingPart.getUnitTonnage());
        assertEquals(equipmentPart.getType(), missingPart.getType());
        assertEquals(equipmentPart.getEquipmentNum(), missingPart.getEquipmentNum());
        assertEquals(equipmentPart.getSize(), missingPart.getSize(), 0.001);
        assertEquals(equipmentPart.getTonnage(), missingPart.getTonnage(), 0.001);
        assertEquals(equipmentPart.isOmniPodded(), missingPart.isOmniPodded());
        assertEquals(equipmentPart.getCampaign(), missingPart.getCampaign());

        isOmniPodded = true;
        equipmentPart = new EquipmentPart(tonnage, type, equipmentNum, size, isOmniPodded, mockCampaign);

        missingPart = equipmentPart.getMissingPart();
        assertNotNull(missingPart);

        assertEquals(equipmentPart.getUnitTonnage(), missingPart.getUnitTonnage());
        assertEquals(equipmentPart.getType(), missingPart.getType());
        assertEquals(equipmentPart.getEquipmentNum(), missingPart.getEquipmentNum());
        assertEquals(equipmentPart.getSize(), missingPart.getSize(), 0.001);
        assertEquals(equipmentPart.getTonnage(), missingPart.getTonnage(), 0.001);
        assertEquals(equipmentPart.isOmniPodded(), missingPart.isOmniPodded());
        assertEquals(equipmentPart.getCampaign(), missingPart.getCampaign());
    }

    @Test
    public void isPartForEquipmentTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        int location = Aero.LOC_NOSE;
        Mounted mounted = mock(Mounted.class);
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setUnit(unit);

        assertTrue(equipmentPart.isPartForEquipmentNum(equipmentNum, location));
        assertFalse(equipmentPart.isPartForEquipmentNum(equipmentNum, Aero.LOC_RWING));
        assertFalse(equipmentPart.isPartForEquipmentNum(equipmentNum - 1, location));
    }

    @Test
    public void isOmniPoddableTest() {
        Campaign mockCampaign = mock(Campaign.class);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        // Not MiscType or WeaponType
        EquipmentPart equipmentPart = new EquipmentPart(75, type, 16, size, false, mockCampaign);
        assertTrue(equipmentPart.isOmniPoddable());

        // If fixed only, then we're not omnipoddable
        when(type.isOmniFixedOnly()).thenReturn(true);
        assertFalse(equipmentPart.isOmniPoddable());

        // MiscType
        MiscType miscType = mock(MiscType.class);
        doReturn(1.0).when(miscType).getTonnage(any(), anyDouble());
        equipmentPart = new EquipmentPart(75, miscType, 16, size, false, mockCampaign);

        // Just because we're MiscType doesn't mean we're omnipoddable ...
        assertFalse(equipmentPart.isOmniPoddable());

        // ... we need to be Mech Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return MiscType.F_MECH_EQUIPMENT.equals(flag);
        }).when(miscType).hasFlag(any());
        assertTrue(equipmentPart.isOmniPoddable());

        // ... or Tank Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return MiscType.F_TANK_EQUIPMENT.equals(flag);
        }).when(miscType).hasFlag(any());
        assertTrue(equipmentPart.isOmniPoddable());

        // ... or Aero Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return MiscType.F_FIGHTER_EQUIPMENT.equals(flag);
        }).when(miscType).hasFlag(any());
        assertTrue(equipmentPart.isOmniPoddable());

        // WeaponType
        WeaponType weaponType = mock(WeaponType.class);
        doReturn(1.0).when(weaponType).getTonnage(any(), anyDouble());
        equipmentPart = new EquipmentPart(75, weaponType, 16, size, false, mockCampaign);

        // Just because we're WeaponType doesn't mean we're omnipoddable ...
        assertFalse(equipmentPart.isOmniPoddable());

        // ... we need to be Mech Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_MECH_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        assertTrue(equipmentPart.isOmniPoddable());

        // ... or Tank Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_TANK_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        assertTrue(equipmentPart.isOmniPoddable());

        // ... or Fighter Equipment ...
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_AERO_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        assertTrue(equipmentPart.isOmniPoddable());

        // ... but not Capital scale.
        doAnswer(inv -> {
            BigInteger flag = inv.getArgument(0);
            return WeaponType.F_AERO_WEAPON.equals(flag);
        }).when(weaponType).hasFlag(any());
        when(weaponType.isCapital()).thenReturn(true);
        assertFalse(equipmentPart.isOmniPoddable());
    }

    @Test
    public void setUnitUpdatesEquipmentTonnage() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        EquipmentPart equipmentPart = new EquipmentPart(75, type, 6, size, false, mockCampaign);

        equipmentPart.setUnit(unit);

        // Ensure we update the equipment tonnage for variable sized equipment
        verify(type, times(1)).getTonnage(eq(entity), eq(size));
    }

    @Test
    public void getLocationTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // No unit
        assertEquals(Entity.LOC_NONE, equipmentPart.getLocation());

        // Assign to a unit
        equipmentPart.setUnit(unit);

        // No equipment at the equipment num
        assertEquals(Entity.LOC_NONE, equipmentPart.getLocation());

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_RT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Our location should match up
        assertEquals(location, equipmentPart.getLocation());
    }

    @Test
    public void getLocationNameTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // No unit
        assertNull(equipmentPart.getLocationName());

        // Assign to a unit
        equipmentPart.setUnit(unit);

        // No equipment at the equipment num
        assertNull(equipmentPart.getLocationName());

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        String locationName = "Mech Right Torso";
        int location = Mech.LOC_RT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(locationName).when(entity).getLocationName(eq(location));

        // Our location should match up
        assertEquals(locationName, equipmentPart.getLocationName());

        // The mount has no named location
        when(mounted.getLocation()).thenReturn(Entity.LOC_NONE);
        assertNull(equipmentPart.getLocationName());
    }

    @Test
    public void isInLocationTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        String locationName = "Mech Right Torso";

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // No unit
        assertFalse(equipmentPart.isInLocation(locationName));

        // Assign to a unit
        equipmentPart.setUnit(unit);

        // No equipment at the equipment num
        assertFalse(equipmentPart.isInLocation(locationName));

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_RT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        doReturn(location).when(entity).getLocationFromAbbr(eq(locationName));

        // Our location should match up
        assertTrue(equipmentPart.isInLocation(locationName));

        // The mount has no named location
        when(mounted.getLocation()).thenReturn(Entity.LOC_NONE);
        assertFalse(equipmentPart.isInLocation(locationName));

        // Split the mount and have the second location be the one we want
        when(mounted.getLocation()).thenReturn(Mech.LOC_RLEG);
        when(mounted.isSplit()).thenReturn(true);
        when(mounted.getSecondLocation()).thenReturn(location);

        assertTrue(equipmentPart.isInLocation(locationName));
    }

    @Test
    public void isRearFacingTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // No unit
        assertFalse(equipmentPart.isRearFacing());

        // Assign to a unit
        equipmentPart.setUnit(unit);

        // No equipment at the equipment num
        assertFalse(equipmentPart.isRearFacing());

        // Put a mount behind the equipment on the unit
        Mounted mounted = mock(Mounted.class);
        when(mounted.isRearMounted()).thenReturn(true);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Our facing should match up
        assertTrue(equipmentPart.isRearFacing());

        when(mounted.isRearMounted()).thenReturn(false);

        // Our facing should match up
        assertFalse(equipmentPart.isRearFacing());
    }

    @Test
    public void equipmentPartWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        EquipmentType type = getEquipmentType(EquipmentTypeLookup.JUMP_JET);
        Campaign mockCampaign = mock(Campaign.class);
        EquipmentPart equipmentPart = new EquipmentPart(65, type, 42, 18.0, false, mockCampaign);
        equipmentPart.setId(25);

        // Write the EquipmentPart XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        equipmentPart.writeToXML(pw, 0);

        // Get the EquipmentPart XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the EquipmentPart
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(EquipmentPart.class, deserializedPart);

        EquipmentPart deserialized = (EquipmentPart) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(equipmentPart.getId(), deserialized.getId());
        assertEquals(equipmentPart.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(equipmentPart.getType(), deserialized.getType());
        assertEquals(equipmentPart.getName(), deserialized.getName());
        assertEquals(equipmentPart.getSize(), deserialized.getSize(), 0.001);
        assertEquals(equipmentPart.getTonnage(), deserialized.getTonnage(), 0.001);
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

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(25);
        equipmentPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(equipmentPart);

        // Remove the part (not salvage)
        equipmentPart.remove(false);

        assertTrue(equipmentPart.getId() < 0);
        assertTrue(equipmentPart.getEquipmentNum() < 0);
        assertNull(equipmentPart.getUnit());
        assertFalse(warehouse.getParts().contains(equipmentPart));

        verify(mounted, times(1)).setHit(eq(true));
        verify(mounted, times(1)).setDestroyed(eq(true));
        verify(mounted, times(1)).setRepairable(eq(false));

        verify(unit, times(1)).destroySystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum));
        verify(unit, times(1)).removePart(eq(equipmentPart));

        ArgumentCaptor<Part> missingPartCaptor = ArgumentCaptor.forClass(Part.class);
        verify(unit, times(1)).addPart(missingPartCaptor.capture());

        Part missingPart = missingPartCaptor.getValue();
        assertInstanceOf(MissingEquipmentPart.class, missingPart);

        MissingEquipmentPart missingEquipmentPart = (MissingEquipmentPart) missingPart;
        assertTrue(missingEquipmentPart.getId() > 0);
        assertEquals(equipmentNum, missingEquipmentPart.getEquipmentNum());
        assertEquals(unit, missingEquipmentPart.getUnit());
        assertTrue(warehouse.getParts().contains(missingEquipmentPart));
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

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(25);
        equipmentPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(equipmentPart);

        // Salvage the part
        equipmentPart.remove(true);

        assertTrue(equipmentPart.getId() > 0);
        assertTrue(equipmentPart.getEquipmentNum() < 0);
        assertNull(equipmentPart.getUnit());
        assertTrue(warehouse.getParts().contains(equipmentPart));

        verify(mounted, times(1)).setHit(eq(true));
        verify(mounted, times(1)).setDestroyed(eq(true));
        verify(mounted, times(1)).setRepairable(eq(false));

        verify(unit, times(1)).destroySystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum));
        verify(unit, times(1)).removePart(eq(equipmentPart));

        ArgumentCaptor<Part> missingPartCaptor = ArgumentCaptor.forClass(Part.class);
        verify(unit, times(1)).addPart(missingPartCaptor.capture());

        Part missingPart = missingPartCaptor.getValue();
        assertInstanceOf(MissingEquipmentPart.class, missingPart);

        MissingEquipmentPart missingEquipmentPart = (MissingEquipmentPart) missingPart;
        assertTrue(missingEquipmentPart.getId() > 0);
        assertEquals(equipmentNum, missingEquipmentPart.getEquipmentNum());
        assertEquals(unit, missingEquipmentPart.getUnit());
        assertTrue(warehouse.getParts().contains(missingEquipmentPart));
    }

    @Test
    public void needsFixingTest() {
        Campaign mockCampaign = mock(Campaign.class);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        EquipmentPart equipmentPart = new EquipmentPart(75, type, 6, size, false, mockCampaign);

        assertFalse(equipmentPart.needsFixing());

        equipmentPart.setHits(1);
        assertTrue(equipmentPart.needsFixing());

        equipmentPart.setHits(7);
        assertTrue(equipmentPart.needsFixing());

        equipmentPart.setHits(0);
        assertFalse(equipmentPart.needsFixing());
    }

    @Test
    public void isMountedOnDestroyedLocationTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // No unit ...
        assertFalse(equipmentPart.isMountedOnDestroyedLocation());

        equipmentPart.setUnit(unit);

        // No mount ....
        assertFalse(equipmentPart.isMountedOnDestroyedLocation());

        // add the equipment
        Mounted mounted = mock(Mounted.class);
        int location = Aero.LOC_LWING;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Destroy the location ...
        doReturn(true).when(unit).isLocationDestroyed(eq(location));
        assertTrue(equipmentPart.isMountedOnDestroyedLocation());

        // Fix the location ...
        doReturn(false).when(unit).isLocationDestroyed(eq(location));
        assertFalse(equipmentPart.isMountedOnDestroyedLocation());

        // Destroy the secondary location ...
        int secondLocation = Aero.LOC_FUSELAGE;
        when(mounted.getSecondLocation()).thenReturn(secondLocation);
        when(mounted.isSplit()).thenReturn(true);
        doReturn(true).when(unit).isLocationDestroyed(eq(secondLocation));
        assertTrue(equipmentPart.isMountedOnDestroyedLocation());

        // Fix them both
        doReturn(false).when(unit).isLocationDestroyed(eq(location));
        doReturn(false).when(unit).isLocationDestroyed(eq(secondLocation));
        assertFalse(equipmentPart.isMountedOnDestroyedLocation());
    }

    @Test
    public void onBadHipOrShoulderTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // Not on unit
        assertFalse(equipmentPart.onBadHipOrShoulder());

        // No equipment mounted at that index
        equipmentPart.setUnit(unit);
        assertFalse(equipmentPart.onBadHipOrShoulder());

        // Mount equipment at the index
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_LARM;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Just because we've got the correct mount, doesn't mean we're
        // on a bad hip or shoulder
        assertFalse(equipmentPart.onBadHipOrShoulder());

        // Bust the shoulder/hip
        doReturn(true).when(unit).hasBadHipOrShoulder(eq(location));
        assertTrue(equipmentPart.onBadHipOrShoulder());

        // Swap over to the secondary location
        doReturn(false).when(unit).hasBadHipOrShoulder(eq(location));
        int secondLocation = Mech.LOC_LT;
        when(mounted.getSecondLocation()).thenReturn(secondLocation);
        when(mounted.isSplit()).thenReturn(true);
        doReturn(true).when(unit).hasBadHipOrShoulder(eq(secondLocation));

        // Still busted
        assertTrue(equipmentPart.onBadHipOrShoulder());

        // But wait, fixed again
        doReturn(false).when(unit).hasBadHipOrShoulder(eq(location));
        doReturn(false).when(unit).hasBadHipOrShoulder(eq(secondLocation));
        assertFalse(equipmentPart.onBadHipOrShoulder());
    }

    @Test
    public void checkFixableTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // Not on unit
        assertNull(equipmentPart.checkFixable());

        // No equipment mounted at that index
        equipmentPart.setUnit(unit);
        assertNull(equipmentPart.checkFixable());

        // Salvaging
        when(unit.isSalvage()).thenReturn(true);
        assertNull(equipmentPart.checkFixable());

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
        assertNull(equipmentPart.checkFixable());

        // Location breached
        doReturn(true).when(unit).isLocationBreached(eq(location));
        doReturn(false).when(unit).isLocationDestroyed(eq(location));
        assertNotNull(equipmentPart.checkFixable());

        // Location destroyed
        doReturn(false).when(unit).isLocationBreached(eq(location));
        doReturn(true).when(unit).isLocationDestroyed(eq(location));
        // CAW: this should be non-null in a perfect world, but because
        //      MekHQ automagically switches to salvage mode when the
        //      location is destroyed, this should return null.
        //      See: https://github.com/MegaMek/mekhq/issues/2387
        assertNull(equipmentPart.checkFixable());

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
        assertNotNull(equipmentPart.checkFixable());

        // Location destroyed
        doReturn(false).when(unit).isLocationBreached(eq(secondaryLocation));
        doReturn(true).when(unit).isLocationDestroyed(eq(secondaryLocation));
        // CAW: this should be non-null in a perfect world, but because
        //      MekHQ automagically switches to salvage mode when the
        //      location is destroyed, this should return null.
        //      See: https://github.com/MegaMek/mekhq/issues/2387
        assertNull(equipmentPart.checkFixable());

        // Restore both locations
        doReturn(false).when(unit).isLocationBreached(eq(location));
        doReturn(false).when(unit).isLocationDestroyed(eq(location));
        doReturn(false).when(unit).isLocationBreached(eq(secondaryLocation));
        doReturn(false).when(unit).isLocationDestroyed(eq(secondaryLocation));

        assertNull(equipmentPart.checkFixable());
    }

    @Test
    public void fixTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(25);
        equipmentPart.setUnit(unit);

        // Damage the part
        equipmentPart.setHits(3);

        // Fix the part
        equipmentPart.fix();

        assertEquals(0, equipmentPart.getHits());
        assertFalse(equipmentPart.needsFixing());

        verify(mounted, times(1)).setHit(eq(false));
        verify(mounted, times(1)).setMissing(eq(false));
        verify(mounted, times(1)).setDestroyed(eq(false));

        verify(unit, times(1)).repairSystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum));
    }

    @Test
    public void updateConditionFromPartWorkingTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // No unit? This is a no-op
        equipmentPart.updateConditionFromPart();

        equipmentPart.setUnit(unit);

        // No equipment mounted at equipmentNum? This is a no-op
        equipmentPart.updateConditionFromPart();

        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Functional equipment mounted
        equipmentPart.updateConditionFromPart();

        verify(mounted, times(1)).setMissing(eq(false));
        verify(mounted, times(1)).setHit(eq(false));
        verify(mounted, times(1)).setDestroyed(eq(false));
        verify(mounted, times(1)).setRepairable(eq(true));
        verify(unit, times(1)).repairSystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum));
    }

    @Test
    public void updateConditionFromPartHitTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setUnit(unit);

        Mounted mounted = mock(Mounted.class);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        // Hit the part
        int hits = 3;
        equipmentPart.setHits(hits);

        equipmentPart.updateConditionFromPart();

        verify(mounted, times(1)).setMissing(eq(false));
        verify(mounted, times(1)).setHit(eq(true));
        verify(mounted, times(1)).setDestroyed(eq(true));
        verify(mounted, times(1)).setRepairable(eq(true));
        verify(unit, times(1)).damageSystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(hits));
    }

    @Test
    public void updateConditionFromEntityNoUnitOrMountedTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, true, mockCampaign);

        // No unit? This is a no-op
        equipmentPart.updateConditionFromEntity(false);

        assertTrue(equipmentPart.isOmniPodded());
        assertEquals(0, equipmentPart.getHits());

        equipmentPart.updateConditionFromEntity(true);

        assertTrue(equipmentPart.isOmniPodded());
        assertEquals(0, equipmentPart.getHits());

        equipmentPart.setUnit(unit);

        // No equipment mounted at equipmentNum? This is a no-op
        equipmentPart.updateConditionFromEntity(false);

        assertTrue(equipmentPart.isOmniPodded());
        assertEquals(0, equipmentPart.getHits());

        equipmentPart.updateConditionFromEntity(true);

        assertTrue(equipmentPart.isOmniPodded());
        assertEquals(0, equipmentPart.getHits());
    }

    @Test
    public void updateConditionFromEntityResetsHitsTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        when(mounted.isMissing()).thenReturn(false);
        int location = Mech.LOC_LLEG;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(1).when(entity).getDamagedCriticals(anyInt(), anyInt(), anyInt()); // Setup damage everywhere else
        doReturn(0).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(location));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, true, mockCampaign);
        equipmentPart.setUnit(unit);

        int hits = 3;
        equipmentPart.setHits(hits);

        // The underlying equipment is fine so this should restore the part
        equipmentPart.updateConditionFromEntity(false);

        assertEquals(0, equipmentPart.getHits());

        // ... and it should learn that the mount is not on an omnipod.
        assertFalse(equipmentPart.isOmniPodded());

        // If the part is split it should also take those hits into account
        when(mounted.isSplit()).thenReturn(true);
        int secondLocation = Mech.LOC_LT;
        when(mounted.getSecondLocation()).thenReturn(secondLocation);
        doReturn(0).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(secondLocation));

        // Break the part again
        equipmentPart.setHits(hits);

        // The underlying equipment in both locations is fine so this should restore the part
        equipmentPart.updateConditionFromEntity(false);

        assertEquals(0, equipmentPart.getHits());
    }

    @Test
    public void updateConditionFromEntityTakesHitsTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        when(mounted.isMissing()).thenReturn(false);
        int location = Mech.LOC_LLEG;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(0).when(entity).getDamagedCriticals(anyInt(), anyInt(), anyInt()); // Setup damage everywhere else
        doReturn(1).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(location));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, true, mockCampaign);
        equipmentPart.setUnit(unit);

        // The underlying equipment has a hit so this should hit the part
        equipmentPart.updateConditionFromEntity(false);

        assertEquals(1, equipmentPart.getHits());

        // If the part is split it should also take those hits into account
        when(mounted.isSplit()).thenReturn(true);
        int secondLocation = Mech.LOC_LT;
        when(mounted.getSecondLocation()).thenReturn(secondLocation);
        doReturn(2).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(secondLocation));

        // Fix the part from our side
        equipmentPart.setHits(0);

        // The underlying equipment in both locations has hits so this should hit the part
        equipmentPart.updateConditionFromEntity(false);

        assertEquals(3, equipmentPart.getHits());
    }

    @Test
    public void updateConditionFromEntityTakesHitsChecksDestructionTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);
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

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        when(mounted.isMissing()).thenReturn(false);
        int location = Mech.LOC_LLEG;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(0).when(entity).getDamagedCriticals(anyInt(), anyInt(), anyInt()); // Setup damage everywhere else
        doReturn(1).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(location));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, true, mockCampaign);
        equipmentPart.setId(16);
        equipmentPart.setUnit(unit);

        warehouse.addPart(equipmentPart);

        MMRandom rng = mock(MMRandom.class);
        try {
            Compute.setRNG(rng);

            // Setup two rolls: PASS and FAIL
            MMRoll roll = mock(MMRoll.class);
            when(roll.getIntValue()).thenReturn(12, 2);
            doReturn(roll).when(rng).d6(eq(2));
            when(campaignOptions.getDestroyPartTarget()).thenReturn(6);

            // The underlying equipment has a hit so this should hit the part
            equipmentPart.updateConditionFromEntity(true);

            // Because we rolled a 12, we should have a hit, but not be destroyed
            assertEquals(1, equipmentPart.getHits());
            assertTrue(warehouse.getParts().contains(equipmentPart));

            // Restore thte first location
            doReturn(0).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(location));

            // Split the mount and bust the second location ...
            when(mounted.isSplit()).thenReturn(true);
            int secondLocation = Mech.LOC_LT;
            when(mounted.getSecondLocation()).thenReturn(secondLocation);
            doReturn(1).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(secondLocation));

            // The underlying equipment has a hit so this should hit the part
            equipmentPart.updateConditionFromEntity(true);

            // Because we did not accrue any additional hits, we should have a hit, but not be destroyed
            assertEquals(1, equipmentPart.getHits());
            assertTrue(warehouse.getParts().contains(equipmentPart));

            // Now, hit both locations hard, triggering a roll we'll fail
            doReturn(2).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(location));
            doReturn(3).when(entity).getDamagedCriticals(eq(CriticalSlot.TYPE_EQUIPMENT), eq(equipmentNum), eq(secondLocation));

            // The underlying equipment has a hit so this should hit the part
            equipmentPart.updateConditionFromEntity(true);

            // Because we failed the roll, we'll be removed and destroyed
            assertEquals(5, equipmentPart.getHits());
            assertFalse(warehouse.getParts().contains(equipmentPart));
        } finally {
            // Restore the RNG for other tests
            Compute.setRNG(MMRandom.R_DEFAULT);
        }
    }

    @Test
    public void updateConditionFromEntityMissingTest() {
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

        double size = 3.0;
        EquipmentType type = mock(EquipmentType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int equipmentNum = 42;
        Mounted mounted = mock(Mounted.class);
        when(mounted.isMissing()).thenReturn(true);
        int location = Mech.LOC_LLEG;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(19);
        equipmentPart.setUnit(unit);

        warehouse.addPart(equipmentPart);

        // The mounted is missing, this should remove the part
        equipmentPart.updateConditionFromEntity(false);

        assertTrue(equipmentPart.getId() < 0);
        assertNull(equipmentPart.getUnit());
        assertFalse(warehouse.getParts().contains(equipmentPart));
    }

    @Test
    public void getBaseTimeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        MiscType type = mock(MiscType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        EquipmentPart equipmentPart = new EquipmentPart(75, type, 42, size, false, mockCampaign);
        equipmentPart.setUnit(unit);

        // Salvaging is 120 minutes ...
        when(unit.isSalvage()).thenReturn(true);
        assertEquals(120, equipmentPart.getBaseTime());

        // ... except when omni-podded.
        equipmentPart.setOmniPodded(true);
        assertEquals(30, equipmentPart.getBaseTime());

        when(unit.isSalvage()).thenReturn(false);

        // If not salvaging, go by hits ...

        // ... no hits, no time.
        assertEquals(0, equipmentPart.getBaseTime());

        // Hits go 100, 150, 200, 250 (>3)
        equipmentPart.setHits(1);
        assertEquals(100, equipmentPart.getBaseTime());
        equipmentPart.setHits(2);
        assertEquals(150, equipmentPart.getBaseTime());
        equipmentPart.setHits(3);
        assertEquals(200, equipmentPart.getBaseTime());
        equipmentPart.setHits(4);
        assertEquals(250, equipmentPart.getBaseTime());
        equipmentPart.setHits(10);
        assertEquals(250, equipmentPart.getBaseTime());

        // Finally, bomb bays on LAMs take 60 minutes
        doReturn(true).when(type).hasFlag(MiscType.F_BOMB_BAY);

        // No hits, no time on the bomb bay.
        equipmentPart.setHits(0);
        assertEquals(0, equipmentPart.getBaseTime());

        // Otherwise, its always 60 minutes
        equipmentPart.setHits(3);
        assertEquals(60, equipmentPart.getBaseTime());
    }

    @Test
    public void getDifficultyTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        double size = 3.0;
        MiscType type = mock(MiscType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        EquipmentPart equipmentPart = new EquipmentPart(75, type, 42, size, false, mockCampaign);
        equipmentPart.setUnit(unit);

        // Salvaging is +0
        when(unit.isSalvage()).thenReturn(true);
        assertEquals(0, equipmentPart.getDifficulty());

        // ... even when omni-podded.
        equipmentPart.setOmniPodded(true);
        assertEquals(0, equipmentPart.getDifficulty());

        when(unit.isSalvage()).thenReturn(false);

        // If not salvaging, go by hits ...

        // ... no hits, no difficulty mod.
        assertEquals(0, equipmentPart.getDifficulty());

        // Hits go -3, -2, 0, +2 (>3)
        equipmentPart.setHits(1);
        assertEquals(-3, equipmentPart.getDifficulty());
        equipmentPart.setHits(2);
        assertEquals(-2, equipmentPart.getDifficulty());
        equipmentPart.setHits(3);
        assertEquals(0, equipmentPart.getDifficulty());
        equipmentPart.setHits(4);
        assertEquals(2, equipmentPart.getDifficulty());
        equipmentPart.setHits(10);
        assertEquals(2, equipmentPart.getDifficulty());

        // Finally, bomb bays on LAMs have a fixed -1 difficulty
        doReturn(true).when(type).hasFlag(MiscType.F_BOMB_BAY);

        equipmentPart.setHits(0);
        assertEquals(-1, equipmentPart.getDifficulty());

        equipmentPart.setHits(3);
        assertEquals(-1, equipmentPart.getDifficulty());
    }

    @Test
    public void isSamePartTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);

        int equipmentNum = 42;
        double size = 3.0;
        double cost = 12.0;
        MiscType type = mock(MiscType.class);
        when(type.getRawCost()).thenReturn(cost);
        doReturn(1.0).when(type).getTonnage(any(), eq(size));

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);

        // We're the same as ourselves
        assertTrue(equipmentPart.isSamePartType(equipmentPart));

        // We're the same as our clone
        Part otherPart = equipmentPart.clone();
        assertTrue(equipmentPart.isSamePartType(otherPart));
        assertTrue(otherPart.isSamePartType(equipmentPart));

        // We're the same even if unit tonnage differs, as long as our
        // equipment tonnage is the same.
        otherPart = new EquipmentPart(65, type, 42, size, false, mockCampaign);
        assertTrue(equipmentPart.isSamePartType(otherPart));
        assertTrue(otherPart.isSamePartType(equipmentPart));

        // We're not the same if types differ
        MiscType otherType = mock(MiscType.class);
        otherPart = new EquipmentPart(75, otherType, 42, size, false, mockCampaign);
        assertFalse(equipmentPart.isSamePartType(otherPart));
        assertFalse(otherPart.isSamePartType(equipmentPart));

        // We're not the same if sizes differ
        double otherSize = 2.0;
        doReturn(1.75).when(type).getTonnage(any(), eq(otherSize));
        otherPart = new EquipmentPart(75, type, 42, otherSize, false, mockCampaign);
        assertFalse(equipmentPart.isSamePartType(otherPart));
        assertFalse(otherPart.isSamePartType(equipmentPart));

        // We're not the same if one is omni-podded and the other isn't
        otherPart = new EquipmentPart(75, type, 42, size, true, mockCampaign);
        assertFalse(equipmentPart.isSamePartType(otherPart));
        assertFalse(otherPart.isSamePartType(equipmentPart));

        // We're not the same if our sticker prices differ;

        // Setup a type with variable costs
        doReturn(true).when(type).hasFlag(eq(MiscType.F_OFF_ROAD));
        doReturn((double) EquipmentType.COST_VARIABLE).when(type).getRawCost();

        // They're both variable cost, but the same misc type (and not on units)
        equipmentPart.setUnit(null);
        otherPart = new EquipmentPart(75, type, 42, size, false, mockCampaign);
        assertTrue(equipmentPart.isSamePartType(otherPart));
        assertTrue(otherPart.isSamePartType(equipmentPart));

        // Put the variable cost part back on a unit
        Mounted mounted = mock(Mounted.class);
        int location = Mech.LOC_CT;
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(cost * 10.0).when(type).getCost(eq(entity), anyBoolean(), eq(location), eq(size));
        equipmentPart.setUnit(unit);

        // And now the other part is not on a unit, so no location hence different cost
        otherPart = new EquipmentPart(75, type, 42, size, false, mockCampaign);
        assertFalse(equipmentPart.isSamePartType(otherPart));
        assertFalse(otherPart.isSamePartType(equipmentPart));
    }

    @Test
    public void checkWeaponBayOnlyWeaponRemovedTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
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

        double size = 3.0;
        WeaponType type = mock(WeaponType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int location = SmallCraft.LOC_HULL;
        int equipmentNum = 42;
        WeaponMounted mounted = mock(WeaponMounted.class);
        when(mounted.getLocation()).thenReturn(location);
        doAnswer(inv -> {
            when(mounted.isDestroyed()).thenReturn(true);
            return null;
        }).when(mounted).setDestroyed(eq(true));
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        int bayEqNum = 12;
        BayWeapon bayWeaponType = mock(BayWeapon.class);
        WeaponMounted weaponBay = mock(WeaponMounted.class);
        when(weaponBay.getLocation()).thenReturn(location);
        when(weaponBay.getType()).thenReturn(bayWeaponType);
        List<WeaponMounted> bayWeapons = new ArrayList<>();
        bayWeapons.add(mounted);
        when(weaponBay.getBayWeapons()).thenReturn(bayWeapons);
        doReturn(weaponBay).when(entity).getEquipment(eq(bayEqNum));
        doReturn(bayEqNum).when(entity).getEquipmentNum(eq(weaponBay));

        WeaponMounted notOurBay = mock(WeaponMounted.class);
        when(notOurBay.getLocation()).thenReturn(location);
        when(notOurBay.getType()).thenReturn(bayWeaponType);
        when(notOurBay.getBayWeapons()).thenReturn(new Vector<>());

        WeaponMounted notABayWeapon = mock(WeaponMounted.class);
        when(notABayWeapon.getLocation()).thenReturn(location);
        when(notABayWeapon.getType()).thenReturn(mock(WeaponType.class));

        ArrayList<WeaponMounted> bayList = new ArrayList<>();
        bayList.add(mock(WeaponMounted.class));
        bayList.add(notOurBay);
        bayList.add(notABayWeapon);
        bayList.add(weaponBay);

        when(entity.getWeaponBayList()).thenReturn(bayList);

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(25);
        equipmentPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(equipmentPart);

        // Remove the part (not salvage), its the only weapon in the bay
        equipmentPart.remove(false);

        // Ensure we destroyed the bay
        verify(weaponBay, times(1)).setHit(eq(true));
        verify(weaponBay, times(1)).setDestroyed(eq(true));
        verify(weaponBay, times(1)).setRepairable(eq(true));
        verify(unit, times(1)).destroySystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(bayEqNum));
    }

    @Test
    public void checkWeaponBayWeaponRemovedOthersOkayTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
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

        double size = 3.0;
        WeaponType type = mock(WeaponType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int location = SmallCraft.LOC_HULL;
        int equipmentNum = 42;
        WeaponMounted mounted = mock(WeaponMounted.class);
        when(mounted.getLocation()).thenReturn(location);
        doAnswer(inv -> {
            when(mounted.isDestroyed()).thenReturn(true);
            return null;
        }).when(mounted).setDestroyed(eq(true));
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        int otherEqNum = 33;
        WeaponMounted otherMounted = mock(WeaponMounted.class);
        when(otherMounted.getLocation()).thenReturn(location);
        doReturn(otherMounted).when(entity).getEquipment(eq(otherEqNum));

        int bayEqNum = 12;
        BayWeapon bayWeaponType = mock(BayWeapon.class);
        WeaponMounted weaponBay = mock(WeaponMounted.class);
        when(weaponBay.getLocation()).thenReturn(location);
        when(weaponBay.getType()).thenReturn(bayWeaponType);
        List<WeaponMounted> bayWeapons = new ArrayList<>();
        bayWeapons.add(otherMounted);
        bayWeapons.add(mounted);
        when(weaponBay.getBayWeapons()).thenReturn(bayWeapons);
        doReturn(weaponBay).when(entity).getEquipment(eq(bayEqNum));
        doReturn(bayEqNum).when(entity).getEquipmentNum(eq(weaponBay));

        WeaponMounted notOurBay = mock(WeaponMounted.class);
        when(notOurBay.getLocation()).thenReturn(location);
        when(notOurBay.getType()).thenReturn(bayWeaponType);
        when(notOurBay.getBayWeapons()).thenReturn(new Vector<>());

        WeaponMounted notABayWeapon = mock(WeaponMounted.class);
        when(notABayWeapon.getLocation()).thenReturn(location);
        when(notABayWeapon.getType()).thenReturn(mock(WeaponType.class));

        ArrayList<WeaponMounted> bayList = new ArrayList<>();
        bayList.add(mock(WeaponMounted.class));
        bayList.add(notOurBay);
        bayList.add(notABayWeapon);
        bayList.add(weaponBay);

        when(entity.getWeaponBayList()).thenReturn(bayList);

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(25);
        equipmentPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(equipmentPart);

        // Remove the part (not salvage), but there is another weapon in the bay
        equipmentPart.remove(false);

        // Ensure we kept the bay alive
        verify(weaponBay, times(1)).setHit(eq(false));
        verify(weaponBay, times(1)).setMissing(eq(false));
        verify(weaponBay, times(1)).setDestroyed(eq(false));
        verify(unit, times(1)).repairSystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(bayEqNum));
    }

    @Test
    public void checkWeaponBayWeaponRemovedOthersDestroyedTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
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

        double size = 3.0;
        WeaponType type = mock(WeaponType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int location = SmallCraft.LOC_HULL;
        int equipmentNum = 42;
        WeaponMounted mounted = mock(WeaponMounted.class);
        when(mounted.getLocation()).thenReturn(location);
        doAnswer(inv -> {
            when(mounted.isDestroyed()).thenReturn(true);
            return null;
        }).when(mounted).setDestroyed(eq(true));
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        int otherEqNum = 33;
        WeaponMounted otherMounted = mock(WeaponMounted.class);
        when(otherMounted.getLocation()).thenReturn(location);
        when(otherMounted.isDestroyed()).thenReturn(true);
        doReturn(otherMounted).when(entity).getEquipment(eq(otherEqNum));

        int bayEqNum = 12;
        BayWeapon bayWeaponType = mock(BayWeapon.class);
        WeaponMounted weaponBay = mock(WeaponMounted.class);
        when(weaponBay.getLocation()).thenReturn(location);
        when(weaponBay.getType()).thenReturn(bayWeaponType);
        List<WeaponMounted> bayWeapons = new ArrayList<>();
        bayWeapons.add(otherMounted);
        bayWeapons.add(mounted);
        when(weaponBay.getBayWeapons()).thenReturn(bayWeapons);
        doReturn(weaponBay).when(entity).getEquipment(eq(bayEqNum));
        doReturn(bayEqNum).when(entity).getEquipmentNum(eq(weaponBay));

        WeaponMounted notOurBay = mock(WeaponMounted.class);
        when(notOurBay.getLocation()).thenReturn(location);
        when(notOurBay.getType()).thenReturn(bayWeaponType);
        when(notOurBay.getBayWeapons()).thenReturn(new Vector<>());

        WeaponMounted notABayWeapon = mock(WeaponMounted.class);
        when(notABayWeapon.getLocation()).thenReturn(location);
        when(notABayWeapon.getType()).thenReturn(mock(WeaponType.class));

        ArrayList<WeaponMounted> bayList = new ArrayList<>();
        bayList.add(mock(WeaponMounted.class));
        bayList.add(notOurBay);
        bayList.add(notABayWeapon);
        bayList.add(weaponBay);

        when(entity.getWeaponBayList()).thenReturn(bayList);

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(25);
        equipmentPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(equipmentPart);

        // Remove the part (not salvage), and there is another weapon in the bay that is destroyed
        equipmentPart.remove(false);

        // Ensure we destroyed the bay
        verify(weaponBay, times(1)).setHit(eq(true));
        verify(weaponBay, times(1)).setDestroyed(eq(true));
        verify(weaponBay, times(1)).setRepairable(eq(true));
        verify(unit, times(1)).destroySystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(bayEqNum));
    }

    @Test
    public void checkWeaponBayUpdateConditionFromPartGoodWeaponTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
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

        double size = 3.0;
        WeaponType type = mock(WeaponType.class);
        doReturn(1.0).when(type).getTonnage(any(), anyDouble());

        int location = SmallCraft.LOC_HULL;
        int equipmentNum = 42;
        WeaponMounted mounted = mock(WeaponMounted.class);
        when(mounted.getLocation()).thenReturn(location);
        doReturn(mounted).when(entity).getEquipment(eq(equipmentNum));

        int otherEqNum = 33;
        WeaponMounted otherMounted = mock(WeaponMounted.class);
        when(otherMounted.getLocation()).thenReturn(location);
        doReturn(otherMounted).when(entity).getEquipment(eq(otherEqNum));

        int bayEqNum = 12;
        BayWeapon bayWeaponType = mock(BayWeapon.class);
        WeaponMounted weaponBay = mock(WeaponMounted.class);
        when(weaponBay.getLocation()).thenReturn(location);
        when(weaponBay.getType()).thenReturn(bayWeaponType);
        List<WeaponMounted> bayWeapons = new ArrayList<>();
        bayWeapons.add(otherMounted);
        bayWeapons.add(mounted);
        when(weaponBay.getBayWeapons()).thenReturn(bayWeapons);
        doReturn(weaponBay).when(entity).getEquipment(eq(bayEqNum));
        doReturn(bayEqNum).when(entity).getEquipmentNum(eq(weaponBay));

        WeaponMounted notOurBay = mock(WeaponMounted.class);
        when(notOurBay.getLocation()).thenReturn(location);
        when(notOurBay.getType()).thenReturn(bayWeaponType);
        when(notOurBay.getBayWeapons()).thenReturn(new Vector<>());

        WeaponMounted notABayWeapon = mock(WeaponMounted.class);
        when(notABayWeapon.getLocation()).thenReturn(location);
        when(notABayWeapon.getType()).thenReturn(mock(WeaponType.class));

        ArrayList<WeaponMounted> bayList = new ArrayList<>();
        bayList.add(mock(WeaponMounted.class));
        bayList.add(notOurBay);
        bayList.add(notABayWeapon);
        bayList.add(weaponBay);

        when(entity.getWeaponBayList()).thenReturn(bayList);

        EquipmentPart equipmentPart = new EquipmentPart(75, type, equipmentNum, size, false, mockCampaign);
        equipmentPart.setId(25);
        equipmentPart.setUnit(unit);

        // Add the part to the warehouse
        warehouse.addPart(equipmentPart);

        // Update the condition of the entity from the part
        equipmentPart.updateConditionFromPart();

        // Ensure we destroyed the bay
        verify(weaponBay, times(1)).setHit(eq(false));
        verify(weaponBay, times(1)).setMissing(eq(false));
        verify(weaponBay, times(1)).setDestroyed(eq(false));
        verify(unit, times(1)).repairSystem(eq(CriticalSlot.TYPE_EQUIPMENT), eq(bayEqNum));
    }
}

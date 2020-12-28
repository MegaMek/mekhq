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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

public class MekLocationTest {
    @Test
    public void deserializationCtorTest() {
        MekLocation loc = new MekLocation();
        assertNotNull(loc);
    }

    @Test
    public void ctorTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mech.LOC_LLEG;
        int tonnage = 70;
        int structureType = EquipmentType.T_STRUCTURE_ENDO_STEEL;
        boolean isClan = true;
        boolean hasTSM = true;
        boolean isQuad = true;
        boolean hasSensors = true;
        boolean hasLifeSupport = true;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        assertEquals(location, mekLocation.getLoc());
        assertEquals(tonnage, mekLocation.getUnitTonnage());
        assertEquals(hasTSM, mekLocation.isTsm());
        assertEquals(structureType, mekLocation.getStructureType());
        assertEquals(isClan, mekLocation.isClan());
        assertEquals(isQuad, mekLocation.forQuad());
        assertEquals(hasSensors, mekLocation.hasSensors());
        assertEquals(hasLifeSupport, mekLocation.hasLifeSupport());
        assertNotNull(mekLocation.getName());
    }

    @Test
    public void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mech.LOC_LLEG;
        int tonnage = 65;
        int structureType = EquipmentType.T_STRUCTURE_ENDO_STEEL;
        boolean isClan = true;
        boolean hasTSM = true;
        boolean isQuad = true;
        boolean hasSensors = true;
        boolean hasLifeSupport = true;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        MekLocation clone = mekLocation.clone();

        assertEquals(mekLocation.getLoc(), clone.getLoc());
        assertEquals(mekLocation.getUnitTonnage(), clone.getUnitTonnage());
        assertEquals(mekLocation.isTsm(), clone.isTsm());
        assertEquals(mekLocation.getStructureType(), clone.getStructureType());
        assertEquals(mekLocation.isClan(), clone.isClan());
        assertEquals(mekLocation.forQuad(), clone.forQuad());
        assertEquals(mekLocation.hasSensors(), clone.hasSensors());
        assertEquals(mekLocation.hasLifeSupport(), clone.hasLifeSupport());
        assertEquals(mekLocation.getName(), clone.getName());
        assertEquals(mekLocation.getPercent(), clone.getPercent(), 0.001);
        assertEquals(mekLocation.isBlownOff(), clone.isBlownOff());
        assertEquals(mekLocation.isBreached(), clone.isBreached());
    }

    @Test
    public void getMissingPartTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mech.LOC_LT;
        int tonnage = 65;
        int structureType = EquipmentType.T_STRUCTURE_REINFORCED;
        boolean isClan = true;
        boolean hasTSM = false;
        boolean isQuad = true;
        boolean hasSensors = false;
        boolean hasLifeSupport = false;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        MissingMekLocation missing = mekLocation.getMissingPart();

        assertEquals(mekLocation.getLoc(), missing.getLocation());
        assertEquals(mekLocation.getUnitTonnage(), missing.getUnitTonnage());
        assertEquals(mekLocation.isTsm(), missing.isTsm());
        assertEquals(mekLocation.getStructureType(), missing.getStructureType());
        assertEquals(mekLocation.isClan(), missing.isClan());
        assertEquals(mekLocation.forQuad(), missing.forQuad());
        assertEquals(mekLocation.getName(), missing.getName());
    }

    @Test
    public void cannotScrapCT() {
        Campaign mockCampaign = mock(Campaign.class);

        MekLocation centerTorso = new MekLocation(Mech.LOC_CT, 25, 0, false, false, false, false, false, mockCampaign);

        assertNotNull(centerTorso.checkScrappable());
    }

    @Test
    public void cannotSalvageCT() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(true);
        Mech entity = mock(Mech.class);
        when(entity.getWeight()).thenReturn(65.0);
        when(unit.getEntity()).thenReturn(entity);

        MekLocation centerTorso = new MekLocation(Mech.LOC_CT, 100, 0, false, false, false, false, false, mockCampaign);
        centerTorso.setUnit(unit);

        assertFalse(centerTorso.isSalvaging());

        MekLocation otherLocation = new MekLocation(Mech.LOC_HEAD, 100, 0, false, false, false, false, false, mockCampaign);
        otherLocation.setUnit(unit);

        assertTrue(otherLocation.isSalvaging());
    }

    @Test
    public void onBadHipOrShoulderTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        Mech entity = mock(Mech.class);
        when(entity.getWeight()).thenReturn(65.0);
        when(unit.getEntity()).thenReturn(entity);

        int location = Mech.LOC_RT;
        MekLocation torso = new MekLocation(location, 100, 0, false, false, false, false, false, mockCampaign);

        // Can't be on a bad hip or shoulder if off a unit
        assertFalse(torso.onBadHipOrShoulder());

        torso.setUnit(unit);

        // Not on a bad hip or shoulder if the unit doesn't say so
        assertFalse(torso.onBadHipOrShoulder());

        doReturn(true).when(unit).hasBadHipOrShoulder(location);

        // Now we're on a bad hip or shoulder
        assertTrue(torso.onBadHipOrShoulder());
    }

    @Test
    public void isSamePartTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);

        int location = Mech.LOC_LLEG, otherLocation = Mech.LOC_HEAD;
        int tonnage = 70;
        int structureType = EquipmentType.T_STRUCTURE_ENDO_STEEL,
                otherStructureType = EquipmentType.T_STRUCTURE_REINFORCED;
        boolean isClan = true;
        boolean hasTSM = true;
        boolean isQuad = true;
        boolean hasSensors = true;
        boolean hasLifeSupport = true;
        MekLocation mekLocation = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        assertTrue(mekLocation.isSamePartType(mekLocation));

        // Same as our clone
        Part other = mekLocation.clone();
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Same if structure type is not Endo Steel and we're clan vs not clan
        mekLocation = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_INDUSTRIAL, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_INDUSTRIAL, !isClan, 
            hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Clan and IS Endo Steel differ
        mekLocation = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_ENDO_STEEL, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(location, tonnage, EquipmentType.T_STRUCTURE_ENDO_STEEL, !isClan, 
            hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Restore the original setup
        mekLocation = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        // Different locations
        other = new MekLocation(otherLocation, tonnage, structureType, isClan, 
            hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different tonnage
        other = new MekLocation(location, tonnage + 10, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different structure
        other = new MekLocation(location, tonnage, otherStructureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different TSM
        other = new MekLocation(location, tonnage, structureType, isClan, 
                !hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Arms for quads must match on quad status, but others do not
        mekLocation = new MekLocation(Mech.LOC_RARM, tonnage, structureType, isClan, 
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(Mech.LOC_LARM, tonnage, structureType, isClan, 
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        mekLocation = new MekLocation(Mech.LOC_LARM, tonnage, structureType, isClan, 
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(Mech.LOC_LARM, tonnage, structureType, isClan, 
                hasTSM, true, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        mekLocation = new MekLocation(Mech.LOC_LARM, tonnage, structureType, isClan, 
                hasTSM, false, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(Mech.LOC_LARM, tonnage, structureType, isClan, 
                hasTSM, false, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        mekLocation = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);
        other = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, !isQuad, hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Restore the original setup
        mekLocation = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, hasLifeSupport, mockCampaign);

        // Different Sensors (off unit)
        other = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, !hasSensors, hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Different Life Support (off unit)
        other = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, !hasLifeSupport, mockCampaign);
        assertFalse(mekLocation.isSamePartType(other));
        assertFalse(other.isSamePartType(mekLocation));

        // Put the location on a unit
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn((double) tonnage);
        when(unit.getEntity()).thenReturn(entity);
        mekLocation.setUnit(unit);

        // Different Sensors (on unit)
        other = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, !hasSensors, hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));

        // Different Life Support (on unit)
        other = new MekLocation(location, tonnage, structureType, isClan, 
                hasTSM, isQuad, hasSensors, !hasLifeSupport, mockCampaign);
        assertTrue(mekLocation.isSamePartType(other));
        assertTrue(other.isSamePartType(mekLocation));
    }
    
    @Test
    public void mekLocationWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        Campaign mockCampaign = mock(Campaign.class);
        MekLocation mekLocation = new MekLocation(Mech.LOC_CT, 100, EquipmentType.T_STRUCTURE_INDUSTRIAL, 
                true, true, true, true, true, mockCampaign);
        mekLocation.setId(25);

        // Write the MekLocation XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        mekLocation.writeToXml(pw, 0);

        // Get the MekLocation XML
        String xml = sw.toString();
        assertFalse(xml.trim().isEmpty());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the MekLocation
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version("1.0.0"));
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof MekLocation);

        MekLocation deserialized = (MekLocation) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(mekLocation.getId(), deserialized.getId());
        assertEquals(mekLocation.getName(), deserialized.getName());
        assertEquals(mekLocation.getLoc(), deserialized.getLoc());
        assertEquals(mekLocation.getUnitTonnage(), deserialized.getUnitTonnage());
        assertEquals(mekLocation.getStructureType(), deserialized.getStructureType());
        assertEquals(mekLocation.isClan(), deserialized.isClan());
        assertEquals(mekLocation.isTsm(), deserialized.isTsm());
        assertEquals(mekLocation.forQuad(), deserialized.forQuad());
        assertEquals(mekLocation.hasSensors(), deserialized.hasSensors());
        assertEquals(mekLocation.hasLifeSupport(), deserialized.hasLifeSupport());
    }

    @Test
    public void lamTorsoRemovableOnlyWithMissingAvionicsAndLandingGear() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMech entity = mock(LandAirMech.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        int location = Mech.LOC_RT;
        MekLocation torso = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        torso.setUnit(unit);

        // Mark that we're salvaging the part
        when(unit.isSalvage()).thenReturn(true);

        // Blow off the right arm
        doReturn(true).when(entity).isLocationBad(Mech.LOC_RARM);

        // 2 criticals
        doReturn(2).when(entity).getNumberOfCriticals(eq(location));
        CriticalSlot mockLandingGear = mock(CriticalSlot.class);
        when(mockLandingGear.isEverHittable()).thenReturn(true);
        when(mockLandingGear.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockLandingGear.getIndex()).thenReturn(LandAirMech.LAM_LANDING_GEAR);
        doReturn(mockLandingGear).when(entity).getCritical(eq(location), eq(0));
        CriticalSlot mockAvionics = mock(CriticalSlot.class);
        when(mockAvionics.isEverHittable()).thenReturn(true);
        when(mockAvionics.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockAvionics.getIndex()).thenReturn(LandAirMech.LAM_AVIONICS);
        doReturn(mockAvionics).when(entity).getCritical(eq(location), eq(1));

        // No missing parts
        doAnswer(inv -> {
            return null;
        }).when(unit).findPart(any());

        // We cannot remove this torso
        assertNotNull(torso.checkFixable());
        assertNotNull(torso.checkSalvagable());
        assertNotNull(torso.checkScrappable());

        // Only missing landing gear, avionics are still good
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            return predicate.test(missingLandingGear) ? missingLandingGear : null;
        }).when(unit).findPart(any());

        // We cannot remove this torso
        assertNotNull(torso.checkFixable());
        assertNotNull(torso.checkSalvagable());
        assertNotNull(torso.checkScrappable());

        // Only missing avionics, landing gear is still good
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We cannot remove this torso
        assertNotNull(torso.checkFixable());
        assertNotNull(torso.checkSalvagable());
        assertNotNull(torso.checkScrappable());

        // Missing both Landing Gear and Avionics
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            if (predicate.test(missingLandingGear)) {
                return missingLandingGear;
            }

            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We CAN remove this torso
        assertNull(torso.checkFixable());
        assertNull(torso.checkSalvagable());
        assertNull(torso.checkScrappable());
    }

    @Test
    public void lamHeadRemovableOnlyWithMissingAvionics() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMech entity = mock(LandAirMech.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        int location = Mech.LOC_HEAD;
        MekLocation head = new MekLocation(location, 30, 0, false, false, false, false, false, mockCampaign);
        head.setUnit(unit);

        // Mark that we're salvaging the part
        when(unit.isSalvage()).thenReturn(true);

        // 1 critical
        doReturn(1).when(entity).getNumberOfCriticals(eq(location));
        CriticalSlot mockAvionics = mock(CriticalSlot.class);
        when(mockAvionics.isEverHittable()).thenReturn(true);
        when(mockAvionics.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockAvionics.getIndex()).thenReturn(LandAirMech.LAM_AVIONICS);
        doReturn(mockAvionics).when(entity).getCritical(eq(location), eq(0));

        // No missing parts
        doAnswer(inv -> {
            return null;
        }).when(unit).findPart(any());

        // We cannot remove this head
        assertNotNull(head.checkFixable());
        assertNotNull(head.checkSalvagable());
        assertNotNull(head.checkScrappable());

        // Missing avionics
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            if (predicate.test(missingLandingGear)) {
                return missingLandingGear;
            }

            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We CAN remove this head
        assertNull(head.checkFixable());
        assertNull(head.checkSalvagable());
        assertNull(head.checkScrappable());
    }
}

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
import java.util.Vector;

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

public class MissingLargeCraftAmmoBinTest {
    @Test
    public void deserializationCtorTest() {
        MissingLargeCraftAmmoBin ammoBin = new MissingLargeCraftAmmoBin();
        assertNotNull(ammoBin);
    }

    @Test
    public void missingLargeCraftAmmoBinMassRepairOptionType() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        MissingLargeCraftAmmoBin missingAmmoBin = new MissingLargeCraftAmmoBin(0, ammoType, 18, 25.0, mockCampaign);

        assertEquals(PartRepairType.AMMO, missingAmmoBin.getMassRepairOptionType());
    }

    @Test
    public void getNewPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        MissingLargeCraftAmmoBin missingAmmoBin = new MissingLargeCraftAmmoBin(0, ammoType, 18, 25.0, mockCampaign);

        // Get a new part that represents the missing bin
        AmmoBin newPart = missingAmmoBin.getNewPart();
        assertEquals(missingAmmoBin.getType(), newPart.getType());
        assertTrue(newPart.getEquipmentNum() < 0);
        assertEquals(missingAmmoBin.getFullShots(), newPart.getFullShots());
        assertEquals(missingAmmoBin.getCampaign(), newPart.getCampaign());
        assertEquals(missingAmmoBin.getName(), newPart.getName());
        assertFalse(newPart.isOneShot());
        assertFalse(newPart.isOmniPodded());
    }

    @Test
    public void missingAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Inferno Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        MissingLargeCraftAmmoBin missingAmmoBin = new MissingLargeCraftAmmoBin(0, isSRM2InfernoAmmo, 42, 25.0, mockCampaign);
        missingAmmoBin.setId(25);

        // Write the AmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        missingAmmoBin.writeToXml(pw, 0);

        // Get the AmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.trim().isEmpty());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the AmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version("1.0.0"));
        assertNotNull(deserializedPart);
        assertTrue(deserializedPart instanceof MissingLargeCraftAmmoBin);

        MissingLargeCraftAmmoBin deserialized = (MissingLargeCraftAmmoBin) deserializedPart;

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

        MissingLargeCraftAmmoBin missingAmmoBin = new MissingLargeCraftAmmoBin(0, ammoType, 18, 25.0, mockCampaign);

        // Same type AmmoBin
        LargeCraftAmmoBin replacementBin = new LargeCraftAmmoBin(0, ammoType, -1, 0, 25.0, mockCampaign);

        // Check and see if same type AmmoBin replacement works.
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Use an Ammo with a different munition type
        missingAmmoBin = new MissingLargeCraftAmmoBin(0, otherAmmoType, 18, 25.0, mockCampaign);
        replacementBin = new LargeCraftAmmoBin(0, otherAmmoType, -1, 0, 25.0, mockCampaign);

        // Check and see if same type AmmoBin replacement works.
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertTrue(missingAmmoBin.isAcceptableReplacement(replacementBin, true));
    }

    @Test
    public void isAcceptableReplacementDifferentTypeTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");
        AmmoType otherAmmoType = getAmmoType("ISSRM6 Ammo");

        MissingLargeCraftAmmoBin missingAmmoBin = new MissingLargeCraftAmmoBin(0, ammoType, 18, 25.0, mockCampaign);

        // Different Ammo Type
        LargeCraftAmmoBin replacementBin = new LargeCraftAmmoBin(0, otherAmmoType, -1, 0, 25.0, mockCampaign);

        // Check and see if this replacement fails.
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Same ammo type, different capacity
        missingAmmoBin = new MissingLargeCraftAmmoBin(0, ammoType, 18, 25.0, mockCampaign);
        replacementBin = new LargeCraftAmmoBin(0, ammoType, -1, 0, 35.0, mockCampaign);

        // Check and see if this replacement fails.
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, false));
        assertFalse(missingAmmoBin.isAcceptableReplacement(replacementBin, true));

        // Different AmmoBin type
        AmmoBin otherAmmoBin = mock(AmmoBin.class);
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
        int bayNum = 31;
        MissingLargeCraftAmmoBin missingAmmoBin = new MissingLargeCraftAmmoBin(0, ammoType, equipmentNum, 25.0, mockCampaign);
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
        Mounted bay = mock(Mounted.class);
        Vector<Integer> bayAmmo = new Vector<Integer>();
        bayAmmo.addElement(equipmentNum);
        when(bay.getBayAmmo()).thenReturn(bayAmmo);
        when(entity.getEquipment(bayNum)).thenReturn(bay);
        missingAmmoBin.setUnit(unit);
        missingAmmoBin.setBay(bayNum);
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
        assertTrue(replacementPart instanceof LargeCraftAmmoBin);

        // 2. And the replacement should match the missing ammo bin
        LargeCraftAmmoBin replacementAmmoBin = (LargeCraftAmmoBin) replacementPart;
        assertTrue(replacementAmmoBin.getId() > 0);
        assertEquals(unit, replacementAmmoBin.getUnit());
        assertEquals(ammoType, replacementAmmoBin.getType());
        assertEquals(equipmentNum, replacementAmmoBin.getEquipmentNum());
        assertEquals(missingAmmoBin.isOneShot(), replacementAmmoBin.isOneShot());
        assertEquals(missingAmmoBin.getFullShots(), replacementAmmoBin.getShotsNeeded());
        assertEquals(bay, replacementAmmoBin.getBay());
    }
}

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
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MissingLargeCraftAmmoBinTest {
    @Test
    public void deserializationCtorTest() {
        MissingLargeCraftAmmoBin ammoBin = new MissingLargeCraftAmmoBin();
        assertNotNull(ammoBin);
    }

    @Test
    public void missingLargeCraftAmmoBinMRMSOptionType() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Inferno Ammo");

        MissingLargeCraftAmmoBin missingAmmoBin = new MissingLargeCraftAmmoBin(0, ammoType, 18, 25.0, mockCampaign);

        assertEquals(PartRepairType.AMMUNITION, missingAmmoBin.getMRMSOptionType());
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
        missingAmmoBin.writeToXML(pw, 0);

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
        assertInstanceOf(MissingLargeCraftAmmoBin.class, deserializedPart);

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
        AmmoMounted mounted = mock(AmmoMounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn((Mounted) mounted);
        WeaponMounted bay = mock(WeaponMounted.class);
        List<AmmoMounted> bayAmmo = new ArrayList<>();
        bayAmmo.add(mounted);
        when(bay.getBayAmmo()).thenReturn(bayAmmo);
        when(entity.getEquipment(bayNum)).thenReturn((Mounted) bay);
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
        assertInstanceOf(LargeCraftAmmoBin.class, replacementPart);

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

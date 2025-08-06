/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts.equipment;

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import megamek.Version;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Mounted;
import megamek.common.equipment.AmmoMounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false,
              mockCampaign);

        assertTrue(ammoBin.canNeverScrap());
    }

    @Test
    public void needsMaintenanceTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots();
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false,
              mockCampaign);

        assertFalse(ammoBin.needsMaintenance());
    }

    @Test
    public void battleArmorAmmoBinCtorTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISSRM6 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots();
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false,
              mockCampaign);

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
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, ammoType, equipmentNum, shotsNeeded, false,
              mockCampaign);

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
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, isSRM2InfernoAmmo, 42, isSRM2InfernoAmmo.getShots() - 1,
              false, mockCampaign);
        ammoBin.setId(25);

        // Write the BattleArmorAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(BattleArmorAmmoBin.class, deserializedPart);

        BattleArmorAmmoBin deserialized = (BattleArmorAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void oneShotBattleArmorAmmoBinWriteToXmlTest()
          throws ParserConfigurationException, SAXException, IOException {
        AmmoType isSRM2InfernoAmmo = getAmmoType("ISSRM2 Ammo");
        Campaign mockCampaign = mock(Campaign.class);
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, isSRM2InfernoAmmo, 42, 0, true, mockCampaign);
        ammoBin.setId(25);

        // Write the BattleArmorAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(BattleArmorAmmoBin.class, deserializedPart);

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
        ammoBin.writeToXML(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(BattleArmorAmmoBin.class, deserializedPart);

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
        BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0, isSRM2InfernoAmmo, 42, isSRM2InfernoAmmo.getShots(),
              false, mockCampaign);
        ammoBin.setId(25);

        // Write the BattleArmorAmmoBin XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ammoBin.writeToXML(pw, 0);

        // Get the BattleArmorAmmoBin XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element partElt = xmlDoc.getDocumentElement();
        assertEquals("part", partElt.getNodeName());

        // Deserialize the BattleArmorAmmoBin
        Part deserializedPart = Part.generateInstanceFromXML(partElt, new Version());
        assertNotNull(deserializedPart);
        assertInstanceOf(BattleArmorAmmoBin.class, deserializedPart);

        BattleArmorAmmoBin deserialized = (BattleArmorAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Nested
    public class TestLoadedAmmo {
        final int SQUAD_SIZE = 5;
        final int equipmentNum = 42;
        final AmmoType ammoType = getAmmoType("BA-SRM2 Ammo");

        Campaign mockCampaign;
        CampaignOptions mockCampaignOptions;
        Warehouse warehouse;
        Quartermaster quartermaster;
        Unit mockUnit;
        BattleArmor mockEntity;
        AmmoMounted mockMounted;

        @BeforeEach
        public void beforeEach() {
            mockCampaign = mock(Campaign.class);
            mockCampaignOptions = mock(CampaignOptions.class);
            when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
            warehouse = new Warehouse();
            when(mockCampaign.getWarehouse()).thenReturn(warehouse);
            quartermaster = new Quartermaster(mockCampaign);
            when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

            mockUnit = mock(Unit.class);
            mockEntity = mock(BattleArmor.class);
            when(mockEntity.getSquadSize()).thenReturn(SQUAD_SIZE);
            when(mockUnit.getEntity()).thenReturn(mockEntity);
            mockMounted = mock(AmmoMounted.class);
            when(mockMounted.getType()).thenReturn(ammoType);
            when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn((Mounted) mockMounted);
        }


        @Test
        public void loadBinWithJustEnoughSpareAmmo() {
            // ARRANGE
            // Create an Ammo Bin with no ammo ...
            int shotsNeeded = ammoType.getShots() * SQUAD_SIZE;
            BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0,
                  ammoType,
                  equipmentNum,
                  shotsNeeded,
                  false,
                  mockCampaign);

            // ... place the ammo bin on a unit ...
            ammoBin.setUnit(mockUnit);
            when(mockMounted.getBaseShotsLeft()).thenReturn(0);

            // ... and add just enough ammo of the right type to the warehouse ...
            quartermaster.addAmmo(ammoType, ammoType.getShots() * SQUAD_SIZE);


            // ACT
            // ... and try to load it.
            ammoBin.loadBin();

            // ASSERT
            // We should have no shots needed ...
            assertEquals(0, ammoBin.getShotsNeeded());
            verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded / SQUAD_SIZE));

            // ... and no more ammo available in the warehouse
            assertEquals(0, quartermaster.getAmmoAvailable(ammoType));
        }

        @Test
        public void loadBinWithJustInsufficientSpareAmmo() {
            // ARRANGE
            // Create an Ammo Bin with no ammo ...
            int shotsNeeded = ammoType.getShots() * SQUAD_SIZE;
            BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0,
                  ammoType,
                  equipmentNum,
                  shotsNeeded,
                  false,
                  mockCampaign);

            // ... place the ammo bin on a unit ...
            ammoBin.setUnit(mockUnit);
            when(mockMounted.getBaseShotsLeft()).thenReturn(0);

            // ... and add just barely not enough ammo to the warehouse ...
            quartermaster.addAmmo(ammoType, (ammoType.getShots() * SQUAD_SIZE) - 1);


            // ACT
            // ... and try to load it.
            ammoBin.loadBin();

            // ASSERT
            // We should still need ammo ...
            assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
            verify(mockMounted, times(0)).setShotsLeft(eq(shotsNeeded / SQUAD_SIZE));

            // ... and all ammo available in the warehouse
            assertEquals((ammoType.getShots() * SQUAD_SIZE) - 1, quartermaster.getAmmoAvailable(ammoType));
        }

        /**
         * If you partially load BA's ammo and save and reload the game, it will give them a free shot at the Entity
         * level. The BA Entity tracks the shots at a per-squad level, not per individual.
         */
        @Test
        public void loadBinWithPartialSpareAmmo() {
            // ARRANGE
            // Create an Ammo Bin with no ammo ...
            int shotsNeeded = ammoType.getShots() * SQUAD_SIZE * 2;
            BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0,
                  ammoType,
                  equipmentNum,
                  shotsNeeded,
                  false,
                  mockCampaign);

            // ... place the ammo bin on a unit ...
            ammoBin.setUnit(mockUnit);
            when(mockMounted.getBaseShotsLeft()).thenReturn(0);

            // ... and add only enough ammo to load a portion of the battle armor (1 extra ammo) ...
            quartermaster.addAmmo(ammoType, (ammoType.getShots() * SQUAD_SIZE) + 1);


            // ACT
            // ... and try to load it.
            ammoBin.loadBin();

            // ASSERT
            // We should still need some ammo ...
            assertEquals(shotsNeeded - SQUAD_SIZE, ammoBin.getShotsNeeded());
            verify(mockMounted, times(1)).setShotsLeft(eq((shotsNeeded / SQUAD_SIZE) - 1));

            // ... and there's one ammo leftover
            assertEquals(1, quartermaster.getAmmoAvailable(ammoType));
        }

        @Test
        public void loadBinWithBountifulSpareAmmo() {
            // ARRANGE
            // Create an Ammo Bin with no ammo ...
            int shotsNeeded = ammoType.getShots() * SQUAD_SIZE;
            BattleArmorAmmoBin ammoBin = new BattleArmorAmmoBin(0,
                  ammoType,
                  equipmentNum,
                  shotsNeeded,
                  false,
                  mockCampaign);

            // ... place the ammo bin on a unit ...
            ammoBin.setUnit(mockUnit);
            when(mockMounted.getBaseShotsLeft()).thenReturn(0);

            // ... and add lots of extra ammo (we're testing if we can overload the unit) ...
            quartermaster.addAmmo(ammoType, (ammoType.getShots() * SQUAD_SIZE * 10));


            // ACT
            // ... and try to load it.
            ammoBin.loadBin();

            // ASSERT
            // We shouldn't need any more ammo ...
            assertEquals(shotsNeeded - SQUAD_SIZE, ammoBin.getShotsNeeded());
            verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded / SQUAD_SIZE));

            // ... and the correct amount of ammo is left
            assertEquals(ammoType.getShots() * SQUAD_SIZE * 9, quartermaster.getAmmoAvailable(ammoType));
        }
    }
}

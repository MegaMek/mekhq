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
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.AmmoStorage;
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
import java.util.*;

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LargeCraftAmmoBinTest {
    @Test
    public void deserializationCtorTest() {
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin();
        assertNotNull(ammoBin);
    }

    @Test
    public void largeCraftAmmoBinCtorTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots();
        double capacity = 12.0;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        assertEquals(ammoType, ammoBin.getType());
        assertEquals(equipmentNum, ammoBin.getEquipmentNum());
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        assertEquals(capacity, ammoBin.getCapacity(), 0.001);
        assertEquals((int) (ammoType.getShots() * capacity), ammoBin.getFullShots());
        assertEquals(mockCampaign, ammoBin.getCampaign());
    }

    @Test
    public void cloneTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        int equipmentNum = 18;
        int bayNum = 31;
        int shotsNeeded = ammoType.getShots() - 1;
        double capacity = 12.0;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);
        ammoBin.setBay(bayNum);

        // Ensure the clone has all the same stuff
        LargeCraftAmmoBin clone = ammoBin.clone();
        assertEquals(ammoBin.getType(), clone.getType());
        assertEquals(ammoBin.getEquipmentNum(), clone.getEquipmentNum());
        assertEquals(ammoBin.getBayEqNum(), clone.getBayEqNum());
        assertEquals(ammoBin.getShotsNeeded(), clone.getShotsNeeded());
        assertEquals(ammoBin.getFullShots(), clone.getFullShots());
        assertEquals(ammoBin.getCampaign(), clone.getCampaign());
        assertEquals(ammoBin.getName(), clone.getName());
        assertEquals(ammoBin.getCapacity(), clone.getCapacity(), 0.001);
    }

    @Test
    public void getNewPartTest() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        double capacity = 5.0;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, 0, capacity, mockCampaign);

        // Ensure the new part has all the same stuff
        AmmoStorage ammoStorage = ammoBin.getNewPart();
        assertEquals(ammoBin.getType(), ammoStorage.getType());
        assertEquals(ammoBin.getFullShots(), ammoStorage.getShots());
        assertEquals(ammoBin.getCampaign(), ammoStorage.getCampaign());
    }

    @Test
    public void cannotDestroyLargeCraftAmmoBin() {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        int equipmentNum = 18;
        int shotsNeeded = ammoType.getShots() - 1;
        double capacity = 12.0;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        assertNull(ammoBin.getMissingPart());
        assertTrue(ammoBin.canNeverScrap());
    }

    @Test
    public void largeCraftAmmoBinWriteToXmlTest() throws ParserConfigurationException, SAXException, IOException {
        Campaign mockCampaign = mock(Campaign.class);
        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        int equipmentNum = 18;
        int bayNum = 31;
        int shotsNeeded = ammoType.getShots();
        double capacity = 12.0;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);
        ammoBin.setId(25);

        // Setup the unit for the ammo bin
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted bay = mock(Mounted.class);
        when(entity.getEquipment(bayNum)).thenReturn(bay);

        ammoBin.setUnit(unit);
        ammoBin.setBay(bayNum);

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
        assertInstanceOf(LargeCraftAmmoBin.class, deserializedPart);

        LargeCraftAmmoBin deserialized = (LargeCraftAmmoBin) deserializedPart;

        // Check that we deserialized the part correctly.
        assertEquals(ammoBin.getId(), deserialized.getId());
        assertEquals(ammoBin.getEquipmentNum(), deserialized.getEquipmentNum());
        assertEquals(ammoBin.getBayEqNum(), deserialized.getBayEqNum());
        assertEquals(ammoBin.getType(), deserialized.getType());
        assertEquals(ammoBin.getShotsNeeded(), deserialized.getShotsNeeded());
        assertEquals(ammoBin.getCapacity(), deserialized.getCapacity(), 0.001);
        assertEquals(ammoBin.getFullShots(), deserialized.getFullShots());
        assertEquals(ammoBin.getName(), deserialized.getName());
    }

    @Test
    public void getBayReturnsBayMatchingEqNum() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create a missing ammo bin on a unit
        int equipmentNum = 18;
        int bayNum = 31;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, 0, 25.0, mockCampaign);
        Unit unit = mock(Unit.class);
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

        ammoBin.setUnit(unit);

        // Set the bay as if we're in deserialization code
        ammoBin.setBay(bayNum);

        assertEquals(bay, ammoBin.getBay());
    }

    @Test
    public void getBayFallsBackToMatchingWeaponBayIfMissingBayEquipment() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create a large craft ammo bin on a unit, whose bay isn't at the bay number
        int equipmentNum = 18;
        int bayNum = 31;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, 0, 25.0, mockCampaign);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        AmmoMounted mounted = mock(AmmoMounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn((Mounted) mounted);
        WeaponMounted wrongWeaponBay = mock(WeaponMounted.class);
        when(wrongWeaponBay.getBayAmmo()).thenReturn(new Vector<>());
        WeaponMounted weaponBay = mock(WeaponMounted.class);
        List<AmmoMounted> bayAmmo = new ArrayList<>();
        bayAmmo.add(mounted);
        when(weaponBay.getBayAmmo()).thenReturn(bayAmmo);
        when(entity.getEquipment(bayNum)).thenReturn((Mounted) weaponBay);
        ArrayList<WeaponMounted> weaponBays = new ArrayList<>();
        weaponBays.add(wrongWeaponBay);
        weaponBays.add(weaponBay);
        when(entity.getWeaponBayList()).thenReturn(weaponBays);

        // Add the ammo bin to the unit and attach it to a non-existent bay
        ammoBin.setUnit(unit);
        ammoBin.setBay(bayNum);

        assertEquals(weaponBay, ammoBin.getBay());
    }

    @Test
    public void getBayFallsBackToMatchingWeaponBayIfNoBayEquipment() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create a large craft ammo bin on a unit, whose bay isn't at the bay number
        int equipmentNum = 18;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, 0, 25.0, mockCampaign);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        AmmoMounted mounted = mock(AmmoMounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn((Mounted) mounted);
        WeaponMounted wrongWeaponBay = mock(WeaponMounted.class);
        when(wrongWeaponBay.getBayAmmo()).thenReturn(new Vector<>());
        WeaponMounted weaponBay = mock(WeaponMounted.class);
        List<AmmoMounted> bayAmmo = new ArrayList<>();
        bayAmmo.add(mounted);
        when(weaponBay.getBayAmmo()).thenReturn(bayAmmo);
        ArrayList<WeaponMounted> weaponBays = new ArrayList<>();
        weaponBays.add(wrongWeaponBay);
        weaponBays.add(weaponBay);
        when(entity.getWeaponBayList()).thenReturn(weaponBays);
        when(entity.whichBay(equipmentNum)).thenReturn((WeaponMounted) weaponBay);

        // Add the ammo bin to the unit without setting up a bay
        ammoBin.setUnit(unit);

        assertEquals(weaponBay, ammoBin.getBay());
    }

    @Test
    public void getBayReturnsNullIfNoMatch() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an ammo bin on a unit
        int equipmentNum = 18;
        int bayNum = 31;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, 0, 25.0, mockCampaign);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        WeaponMounted bay = mock(WeaponMounted.class);
        when(bay.getBayAmmo()).thenReturn(new ArrayList<>());
        when(entity.getEquipment(bayNum)).thenReturn((Mounted) bay);
        when(entity.getWeaponBayList()).thenReturn(new ArrayList<>());

        // Set the bay without actually setting it (if through deserialization)
        ammoBin.setBay(bayNum);

        ammoBin.setUnit(unit);

        assertNull(ammoBin.getBay());
    }

    @Test
    public void getBayReturnsSetBayValue() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an ammo bin on a unit
        int equipmentNum = 18;
        int bayNum = 31;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, 0, 25.0, mockCampaign);

        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        Mounted bay = mock(Mounted.class);
        when(entity.getEquipment(bayNum)).thenReturn(bay);

        ammoBin.setUnit(unit);
        ammoBin.setBay(bay);

        assertEquals(bay, ammoBin.getBay());
    }

    @Test
    public void bayAvailableCapacityZeroWithoutUnit() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an ammo bin not on a unit ...
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, 0, 25.0, mockCampaign);

        // ... and it should have no available capacity.
        assertEquals(0.0, ammoBin.bayAvailableCapacity(), 0.0);
    }

    @Test
    public void bayAvailableCapacityEmptyOnlyBayOnUnit() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an ammo bin on a unit
        int equipmentNum = 18;
        int bayNum = 31;
        int capacity = 5;
        int shotsNeeded = ammoType.getShots() * capacity;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        Unit unit = mock(Unit.class);
        when(unit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin }));
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        Mounted bay = mock(Mounted.class);
        when(entity.getEquipment(bayNum)).thenReturn(bay);

        ammoBin.setUnit(unit);
        ammoBin.setBay(bay);

        // ... and it should have all available capacity.
        assertEquals(capacity, ammoBin.bayAvailableCapacity(), 0.001);
    }

    @Test
    public void bayAvailableCapacityFullOnlyBayOnUnit() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an ammo bin on a unit
        int equipmentNum = 18;
        int bayNum = 31;
        int capacity = 5;
        int shotsNeeded = 0;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        Unit unit = mock(Unit.class);
        when(unit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin }));
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        Mounted bay = mock(Mounted.class);
        when(entity.getEquipment(bayNum)).thenReturn(bay);

        ammoBin.setUnit(unit);
        ammoBin.setBay(bay);

        // ... and it should have no available capacity.
        assertEquals(0.0, ammoBin.bayAvailableCapacity(), 0.0);
    }

    @Test
    public void bayAvailableCapacityEmptyOnlyBayOfTypeOnUnit() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an ammo bin on a unit
        int equipmentNum = 18;
        int bayNum = 31;
        int capacity = 5;
        int shotsNeeded = ammoType.getShots() * capacity;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // Setup another bin that isn't a match.
        LargeCraftAmmoBin otherBin = mock(LargeCraftAmmoBin.class);
        when(otherBin.getBayEqNum()).thenReturn(bayNum);
        when(otherBin.getType()).thenReturn(getAmmoType("ISLRM15 Ammo"));

        Unit unit = mock(Unit.class);
        when(unit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin, otherBin, }));
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        Mounted bay = mock(Mounted.class);
        when(entity.getEquipment(bayNum)).thenReturn(bay);

        ammoBin.setUnit(unit);
        ammoBin.setBay(bay);

        // ... and it should have all available capacity.
        assertEquals(capacity, ammoBin.bayAvailableCapacity(), 0.001);
    }

    @Test
    public void bayAvailableCapacityFullOnlyBayOfTypeOnUnit() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an ammo bin on a unit
        int equipmentNum = 18;
        int bayNum = 31;
        int capacity = 5;
        int shotsNeeded = 0;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // Setup another bin that isn't a match.
        LargeCraftAmmoBin otherBin = mock(LargeCraftAmmoBin.class);
        when(otherBin.getBayEqNum()).thenReturn(bayNum);
        when(otherBin.getType()).thenReturn(getAmmoType("ISLRM15 Ammo"));

        Unit unit = mock(Unit.class);
        when(unit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin, otherBin, }));
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(ammoType);
        when(entity.getEquipment(equipmentNum)).thenReturn(mounted);
        Mounted bay = mock(Mounted.class);
        when(entity.getEquipment(bayNum)).thenReturn(bay);

        ammoBin.setUnit(unit);
        ammoBin.setBay(bay);

        // ... and it should have no available capacity.
        assertEquals(0.0, ammoBin.bayAvailableCapacity(), 0.0);
    }

    @Test
    public void unloadEmptyBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an empty Ammo Bin...
        int shotsNeeded = ammoType.getShots();
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, 1.0, mockCampaign);

        // ...and unload it.
        ammoBin.unload();

        // Nothing should be added to the Warehouse.
        assertTrue(warehouse.getParts().isEmpty());
    }

    @Test
    public void unloadFullBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create a full Ammo Bin...
        int shotsNeeded = 0;
        int capacity = 3;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, capacity, mockCampaign);

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
        assertEquals(ammoType.getShots() * capacity, added.getShots());
        assertEquals(ammoType.getShots() * capacity, ammoBin.getAmountAvailable());
    }

    @Test
    public void unloadPartialBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with just one round...
        int shotsNeeded = ammoType.getShots() - 1;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, 1.0, mockCampaign);

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

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an empty Ammo Bin...
        int shotsNeeded = ammoType.getShots();
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, 1.0, mockCampaign);

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

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create a full Ammo Bin...
        int shotsNeeded = 0;
        int capacity = 2;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, capacity, mockCampaign);

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
        assertEquals(ammoType.getShots() * capacity, added.getShots());
        assertEquals(ammoType.getShots() * capacity, ammoBin.getAmountAvailable());
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

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with just one round...
        int shotsNeeded = ammoType.getShots() - 1;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, 1.0, mockCampaign);

        // ...and salvage it.
        ammoBin.remove(true);

        // We should now have that ammo in our warehouse
        // and the ammo bin should still be there.
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
    public void unloadSingleTonEmptyBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an empty Ammo Bin...
        int equipmentNum = 18;
        int capacity = 2;
        int shotsNeeded = ammoType.getShots() * capacity;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ...and unload one ton.
        ammoBin.unloadSingleTon();

        // Nothing should be added to the Warehouse.
        assertTrue(warehouse.getParts().isEmpty());
    }

    @Test
    public void unloadSingleTonFullBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create a full Ammo Bin...
        int equipmentNum = 19;
        int shotsNeeded = 0;
        int capacity = 3;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(ammoType.getShots() * capacity);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ...and unload one ton.
        ammoBin.unloadSingleTon();

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

        // And confirm we only removed one ton
        assertEquals(ammoType.getShots() * (capacity - 1), ammoBin.getCurrentShots());
        verify(mockMounted, times(1)).setShotsLeft(eq(ammoType.getShots() * (capacity - 1)));
    }

    @Test
    public void unloadSingleTonPartialBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with three tons...
        int equipmentNum = 19;
        int capacity = 5;
        int capacityNeeded = 2;
        int shotsNeeded = ammoType.getShots() * capacityNeeded;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(ammoType.getShots() * (capacity - capacityNeeded));
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ...and unload only up to a ton.
        ammoBin.unloadSingleTon();

        // We should now have that ammo in our warehouse.
        AmmoStorage added = null;
        for (Part part : warehouse.getParts()) {
            assertNull(added);
            assertInstanceOf(AmmoStorage.class, part);
            added = (AmmoStorage) part;
        }

        // Confirm the added part has the correct values
        assertEquals(ammoType, added.getType());
        assertEquals(ammoType.getShots(), added.getShots());

        // And that the AmmoBin should have two tons remaining
        int expectedShotsLeft = ammoType.getShots() * (capacity - capacityNeeded - 1);
        assertEquals(expectedShotsLeft, ammoBin.getCurrentShots());
        verify(mockMounted, times(1)).setShotsLeft(eq(expectedShotsLeft));
    }

    @Test
    public void loadBinWithoutUnitDoesNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with no ammo...
        int capacity = 10;
        int shotsNeeded = ammoType.getShots() * capacity;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, capacity, mockCampaign);

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

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 7;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

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

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 2;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

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
        AmmoType otherAmmoType = getAmmoType("ISLRM20 Artemis-capable Ammo");
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots() * capacity);

        // ... and try to load it.
        ammoBin.loadBin();

        // We should have not changed how many shots are needed ...
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));

        // ... nor how many shots are available of the wrong type.
        assertEquals(otherAmmoType.getShots() * capacity, quartermaster.getAmmoAvailable(otherAmmoType));
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

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 3;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

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
        quartermaster.addAmmo(ammoType, ammoType.getShots() * capacity);

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

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with plenty of ammo ...
        int capacity = 4;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

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
        int shotsOnHand = 10 * capacity * ammoType.getShots();
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
    public void loadBinSingleTonWithoutUnitDoesNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with no ammo...
        int capacity = 10;
        int shotsNeeded = ammoType.getShots() * capacity;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, capacity, mockCampaign);

        // ...and try to load it when the warehouse is empty.
        ammoBin.loadBinSingleTon();

        // We should have not changed how many shots are needed
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
    }

    @Test
    public void loadBinSingleTonWithoutSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 7;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and try to load it when the warehouse is empty.
        ammoBin.loadBinSingleTon();

        // We should have not changed how many shots are needed
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));
    }

    @Test
    public void loadBinSingleTonithOnlySpareAmmoOfWrongType() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 2;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add ammo of the wrong type to the warehouse ...
        AmmoType otherAmmoType = getAmmoType("ISLRM20 Artemis-capable Ammo");
        quartermaster.addAmmo(otherAmmoType, otherAmmoType.getShots() * capacity);

        // ... and try to load it.
        ammoBin.loadBinSingleTon();

        // We should have not changed how many shots are needed ...
        assertEquals(shotsNeeded, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(0));

        // ... nor how many shots are available of the wrong type.
        assertEquals(otherAmmoType.getShots() * capacity, quartermaster.getAmmoAvailable(otherAmmoType));
    }

    @Test
    public void loadBinSingleTonWithJustEnoughSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 3;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the right type to the warehouse ...
        quartermaster.addAmmo(ammoType, ammoType.getShots());

        // ... and try to load one ton.
        ammoBin.loadBinSingleTon();

        // We should still have some ammo needed ...
        assertEquals(shotsNeeded - ammoType.getShots(), ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(ammoType.getShots()));

        // ... and no more ammo available in the warehouse
        assertEquals(0, quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void loadBinSingleTonWithMoreThanEnoughSpareAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with plenty of ammo ...
        int capacity = 4;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add more than enough ammo of the right type to the warehouse ...
        int shotsOnHand = 10 * capacity * ammoType.getShots();
        quartermaster.addAmmo(ammoType, shotsOnHand);

        // ... and try to load one ton.
        ammoBin.loadBinSingleTon();

        // We should still need some ammo ...
        assertEquals(shotsNeeded - ammoType.getShots(), ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(ammoType.getShots()));

        // ... and only the ammo needed was pulled from the warehouse
        assertEquals(shotsOnHand - ammoType.getShots(), quartermaster.getAmmoAvailable(ammoType));
    }

    @Test
    public void needsFixingEmptyBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an empty Ammo Bin...
        int equipmentNum = 13;
        int capacity = 5;
        int shotsNeeded = ammoType.getShots() * capacity;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin }));
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // Empty bins need fixing.
        assertTrue(ammoBin.needsFixing());
    }

    @Test
    public void needsFixingFullBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create a full Ammo Bin...
        int equipmentNum = 13;
        int shotsNeeded = 0;
        int capacity = 3;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin }));
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // Full bins do not.
        assertFalse(ammoBin.needsFixing());
    }

    @Test
    public void needsFixingPartialBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with just one ton ...
        int equipmentNum = 13;
        int capacity = 3;
        int shotsNeeded = ammoType.getShots() * (capacity - 1);
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin }));
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // Partial bins do need fixing.
        assertTrue(ammoBin.needsFixing());
    }

    @Test
    public void needsFixingOverflowingBinTest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with one ton too many...
        int equipmentNum = 13;
        int capacity = 3;
        int shotsNeeded = -ammoType.getShots();
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin }));
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // Overflowing bins do need fixing.
        assertTrue(ammoBin.needsFixing());
    }

    @Test
    public void fixBinWithoutUnitDoesNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with no ammo...
        int capacity = 3;
        int shotsNeeded = ammoType.getShots() * capacity;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, -1, shotsNeeded, capacity, mockCampaign);

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

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 3;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
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

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 3;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add ammo of the wrong type to the warehouse ...
        AmmoType otherAmmoType = getAmmoType("ISLRM20 Artemis-capable Ammo");
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

        AmmoType ammoType = getAmmoType("ISLRM20 Artemis-capable Ammo");

        // Create an Ammo Bin with no ammo ...
        int capacity = 3;
        int shotsNeeded = ammoType.getShots() * capacity;
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add just enough ammo of the right type to the warehouse ...
        quartermaster.addAmmo(ammoType, shotsNeeded);

        // ... and try to load it.
        ammoBin.fix();

        // We should have loaded one ton ...
        assertEquals(ammoType.getShots() * (capacity - 1), ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(ammoType.getShots()));

        // ... and have more ammo available in the warehouse
        assertEquals(ammoType.getShots() * (capacity - 1), quartermaster.getAmmoAvailable(ammoType));
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

        AmmoType ammoType = getAmmoType("ISLRM20 Ammo");

        // Create an Ammo Bin with plenty of ammo ...
        int capacity = 3;
        int shotsNeeded = ammoType.getShots();
        int equipmentNum = 42;
        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, ammoType, equipmentNum, shotsNeeded, capacity, mockCampaign);

        // ... place the ammo bin on a unit ...
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(ammoType);
        when(mockMounted.getBaseShotsLeft()).thenReturn(0);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        ammoBin.setUnit(mockUnit);

        // ... and add more than enough ammo of the right type to the warehouse ...
        int shotsOnHand = 10 * capacity * ammoType.getShots();
        quartermaster.addAmmo(ammoType, shotsOnHand);

        // ... and try to load it.
        ammoBin.fix();

        // We should have loaded one ton, which is just enough...
        assertEquals(0, ammoBin.getShotsNeeded());
        verify(mockMounted, times(1)).setShotsLeft(eq(shotsNeeded));

        // ... and have more ammo available in the warehouse
        assertEquals(shotsOnHand - ammoType.getShots(), quartermaster.getAmmoAvailable(ammoType));
    }
}

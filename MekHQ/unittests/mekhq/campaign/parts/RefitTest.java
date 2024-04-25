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
package mekhq.campaign.parts;

import megamek.Version;
import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.*;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(value = MockitoExtension.class)
public class RefitTest {
    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @Mock
    private Game mockGame;

    @Mock
    private GameOptions mockGameOptions;

    @Mock
    private Board mockBoard;

    @Mock
    private Quartermaster mockQuartermaster;

    @Mock
    private Warehouse mockWarehouse;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        lenient().when(mockCampaignOptions.getCommonPartPriceMultiplier()).thenReturn(1d);
        lenient().when(mockCampaignOptions.getInnerSphereUnitPriceMultiplier()).thenReturn(1d);
        lenient().when(mockCampaignOptions.getInnerSpherePartPriceMultiplier()).thenReturn(1d);

        lenient().when(mockCampaign.getGame()).thenReturn(mockGame);
        lenient().when(mockGame.getBoard()).thenReturn(mockBoard);
        lenient().when(mockBoard.inSpace()).thenReturn(false);
        lenient().when(mockGame.getOptions()).thenReturn(mockGameOptions);
        lenient().when(mockGameOptions.booleanOption(OptionsConstants.ADVAERORULES_SINGLE_NO_CAP))
                .thenReturn(false);
        lenient().when(mockGameOptions.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER))
                .thenReturn(false);

        lenient().when(mockCampaign.getQuartermaster()).thenReturn(mockQuartermaster);

        lenient().when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
    }

    @Test
    public void deserializationCtor() {
        Refit refit = new Refit();
        assertNotNull(refit);
    }

    @Test
    public void newRefitCtor() {
        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getLocustLCT1V();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getLocustLCT1E();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.initializeParts(false);

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        assertEquals(mockCampaign, refit.getCampaign());

        // Should be old parts...
        assertFalse(refit.getOldUnitParts().isEmpty());

        // ... and new parts.
        assertFalse(refit.getNewUnitParts().isEmpty());

        // ... and we'll need to buy some parts
        assertFalse(refit.getShoppingList().isEmpty());
    }

    @Test
    public void locust1Vto1ETest() {
        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getLocustLCT1V();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getLocustLCT1E();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        assertEquals(mockCampaign, refit.getCampaign());

        //
        // Locust 1V to 1E Class D refit steps (in no particular order):
        // 1. Remove excess Machine Gun (LA) [120 mins]
        // 2. Remove excess Machine Gun (RA) [120 mins]
        // 3. Remove Machine Gun Ammo [Full] Bin (CT) [120 mins]
        // 4. Move Medium Laser (CT) to (RA) [120 mins]
        // 5. Add Medium Laser to (LA) [120 mins]
        // 6. Add Small Laser to (RA) [120 mins]
        // 7. Add Small Laser to (LA) [120 mins]
        //
        // Everything else is the same.
        //

        // Per SO p188:
        // "This kit permits players to install a new item
        // where previously there was none..."
        assertEquals(Refit.CLASS_D, refit.getRefitClass());

        // Time?
        // + 3 removals @ 120 mins ea
        // + 1 move @ 120 mins ea
        // + 3 adds @ 120 mins ea
        // x 3 (Class D)
        assertEquals((120.0 * 7.0) * 3.0, refit.getActualTime(), 0.1);

        // Cost?
        // + 1 Medium Laser @ 40,000 ea
        // + 2 Small Lasers @ 11,250 ea
        // x 1.1 (Refit Kit cost, SO p188)
        assertEquals(Money.of((40000 + 11250 + 11250) * 1.1), refit.getCost());

        // We're removing 2 machine guns and an ammo bin
        List<Part> removedParts = refit.getOldUnitParts();
        assertEquals(3, removedParts.size());
        assertEquals(2, removedParts.stream()
                .filter(p -> (p instanceof EquipmentPart) && p.getName().equals("Machine Gun"))
                .count());
        assertEquals(1, removedParts.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("Machine Gun Ammo [Full] Bin"))
                .count());

        // All of the new parts should be from the old unit
        List<Part> newParts = refit.getNewUnitParts();
        assertTrue(newParts.stream().allMatch(p -> p.getUnit().equals(oldUnit)));

        // We need to buy one Medium Laser and two Small Lasers
        List<Part> shoppingCart = refit.getShoppingList();
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Medium Laser"))
                .count());
        assertEquals(2, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Small Laser"))
                .count());
    }

    @Test
    public void testLocust1Vto1EWriteToXml() throws ParserConfigurationException, SAXException, IOException {
        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getLocustLCT1V();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getLocustLCT1E();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Make sure the unit parts have an ID before we serialize them
        int partId = 1;
        for (Part part : oldUnit.getParts()) {
            part.setId(partId++);
        }

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);

        // Write the Refit XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        refit.writeToXML(pw, 0);

        // Get the Refit XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element refitElt = xmlDoc.getDocumentElement();
        assertEquals("refit", refitElt.getNodeName());

        // Deserialize the refit
        Refit deserialized = Refit.generateInstanceFromXML(refitElt, new Version(), mockCampaign, oldUnit);
        assertNotNull(deserialized);

        // Spot check the values
        assertEquals(refit.getTime(), deserialized.getTime());
        assertEquals(refit.getActualTime(), deserialized.getActualTime());
        assertEquals(refit.getCost(), deserialized.getCost());
        assertEquals(refit.isSameArmorType(), deserialized.isSameArmorType());
        assertEquals(refit.hasFailedCheck(), deserialized.hasFailedCheck());
        assertEquals(refit.getRefitClass(), deserialized.getRefitClass());
        assertEquals(refit.getTimeSpent(), deserialized.getTimeSpent());
        assertEquals(refit.getTimeLeft(), deserialized.getTimeLeft());
        assertEquals(refit.isCustomJob(), deserialized.isCustomJob());
        assertEquals(refit.kitFound(), deserialized.kitFound());
        assertEquals(refit.isBeingRefurbished(), deserialized.isBeingRefurbished());
        assertEquals(refit.getTech(), deserialized.getTech());

        // Check that we got all the correct old parts in the XML
        Set<Integer> oldUnitParts = refit.getOldUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        Set<Integer> serializedOldParts = deserialized.getOldUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        assertEquals(oldUnitParts, serializedOldParts);

        // Check that we got all the correct new parts in the XML
        Set<Integer> newUnitParts = refit.getNewUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        Set<Integer> serializedNewParts = deserialized.getNewUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        assertEquals(newUnitParts, serializedNewParts);

        // Check that we got all the shopping list entries (by name, not amazing but reasonable)
        List<String> shoppingList = refit.getShoppingList().stream()
                .map(Part::getName)
                .collect(Collectors.toList());
        List<String> serializedShoppingList = deserialized.getShoppingList().stream()
                .map(Part::getName)
                .collect(Collectors.toList());

        // Make sure they're the same length first...
        assertEquals(shoppingList.size(), serializedShoppingList.size());

        // ... then make sure they're the "same" by removing them one by one...
        for (String partName : shoppingList) {
            assertTrue(serializedShoppingList.remove(partName));
        }

        // ... and ensuring nothing is left.
        assertTrue(serializedShoppingList.isEmpty());

        // Do the same for their descriptions, which include the quantities...
        List<String> shoppingListDescs = Arrays.asList(refit.getShoppingListDescription());
        // ... except the second list needs to be mutable.
        List<String> serializedShoppingListDescs = new ArrayList<>(
                Arrays.asList(deserialized.getShoppingListDescription()));

        assertEquals(shoppingListDescs.size(), serializedShoppingListDescs.size());
        for (String desc : shoppingListDescs) {
            assertTrue(serializedShoppingListDescs.remove(desc));
        }

        assertTrue(serializedShoppingListDescs.isEmpty());
    }

    @Test
    public void javelinJVN10Nto10ATest() {
        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getJavelinJVN10N();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getJavelinJVN10A();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        assertEquals(mockCampaign, refit.getCampaign());

        //
        // Javelin 10N to 10A Class C refit steps (in no particular order):
        // 1. Remove excess SRM 6 (LT) [120 mins]
        // 2. Remove excess SRM 6 (RT) [120 mins]
        // 3. Remove SRM 6 Ammo Bin (LT) [120 mins]
        // 4. Remove SRM 6 Ammo Bin (RT) [120 mins]
        // 5. Add LRM 15 to (RT) [120 mins]
        // 6. Add LRM 15 Ammo Bin to (RT) [120 mins]
        //
        // Everything else is the same.
        //

        // Per SO p188:
        // "A Class C kit also enables replacement of a weapon
        // or item of equipment with any other, even if it is
        // larger than the item(s) being replaced; for example,
        // replacing an ER large laser with an LRM-10 launcher
        // and ammunition."
        assertEquals(Refit.CLASS_C, refit.getRefitClass());

        // Time?
        // + 4 removals @ 120 mins ea
        // + 2 adds @ 120 mins ea
        // x 2 (Class C)
        assertEquals((120.0 * 6.0) * 2.0, refit.getActualTime(), 0.1);

        // Cost?
        // + 1 LRM 15 @ 175,000 ea
        // + 1 ton LRM 15 Ammo @ 30,000 ea
        // x 1.1 (Refit Kit cost, SO p188)
        assertEquals(Money.of(175000.0 + 30000.0).multipliedBy(1.1), refit.getCost());

        // We're removing 2 SRM 6s and two ammo bins
        List<Part> removedParts = refit.getOldUnitParts();
        assertEquals(4, removedParts.size());
        assertEquals(2, removedParts.stream()
                .filter(p -> (p instanceof EquipmentPart) && p.getName().equals("SRM 6")).count());
        assertEquals(2, removedParts.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("SRM 6 Ammo Bin")).count());

        // All of the new parts should be from the old unit
        List<Part> newParts = refit.getNewUnitParts();
        assertTrue(newParts.stream().allMatch(p -> p.getUnit().equals(oldUnit)));

        // We need to buy one LRM 15 and one LRM 15 Ammo Bin
        List<Part> shoppingCart = refit.getShoppingList();
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("LRM 15"))
                .count());
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("LRM 15 Ammo Bin")).count());
    }

    @Test
    public void testJavelinJVN10Nto10AWriteToXml() throws ParserConfigurationException, SAXException, IOException {
        Person mockTech = mock(Person.class);
        UUID techId = UUID.randomUUID();
        when(mockTech.getId()).thenReturn(techId);

        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getJavelinJVN10N();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getJavelinJVN10A();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Make sure the unit parts have an ID before we serialize them
        int partId = 1;
        for (Part part : oldUnit.getParts()) {
            part.setId(partId++);
        }

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        refit.setTech(mockTech);
        refit.addTimeSpent(60); // 1 hour of work!

        // Write the Refit XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        refit.writeToXML(pw, 0);

        // Get the Refit XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element refitElt = xmlDoc.getDocumentElement();
        assertEquals("refit", refitElt.getNodeName());

        // Deserialize the refit
        Refit deserialized = Refit.generateInstanceFromXML(refitElt, new Version(), mockCampaign, oldUnit);
        assertNotNull(deserialized);

        // Spot check the values
        assertEquals(refit.getTime(), deserialized.getTime());
        assertEquals(refit.getActualTime(), deserialized.getActualTime());
        assertEquals(refit.getCost(), deserialized.getCost());
        assertEquals(refit.isSameArmorType(), deserialized.isSameArmorType());
        assertEquals(refit.hasFailedCheck(), deserialized.hasFailedCheck());
        assertEquals(refit.getRefitClass(), deserialized.getRefitClass());
        assertEquals(refit.getTimeSpent(), deserialized.getTimeSpent());
        assertEquals(refit.getTimeLeft(), deserialized.getTimeLeft());
        assertEquals(refit.isCustomJob(), deserialized.isCustomJob());
        assertEquals(refit.kitFound(), deserialized.kitFound());
        assertEquals(refit.isBeingRefurbished(), deserialized.isBeingRefurbished());
        assertEquals(refit.getTech().getId(), deserialized.getTech().getId());

        // Check that we got all the correct old parts in the XML
        Set<Integer> oldUnitParts = refit.getOldUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        Set<Integer> serializedOldParts = deserialized.getOldUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        assertEquals(oldUnitParts, serializedOldParts);

        // Check that we got all the correct new parts in the XML
        Set<Integer> newUnitParts = refit.getNewUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        Set<Integer> serializedNewParts = deserialized.getNewUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        assertEquals(newUnitParts, serializedNewParts);

        // Check that we got all the shopping list entries (by name, not amazing but reasonable)
        List<String> shoppingList = refit.getShoppingList().stream()
                .map(Part::getName)
                .collect(Collectors.toList());
        List<String> serializedShoppingList = deserialized.getShoppingList().stream()
                .map(Part::getName)
                .collect(Collectors.toList());

        // Make sure they're the same length first...
        assertEquals(shoppingList.size(), serializedShoppingList.size());

        // ... then make sure they're the "same" by removing them one by one...
        for (String partName : shoppingList) {
            assertTrue(serializedShoppingList.remove(partName));
        }

        // ... and ensuring nothing is left.
        assertTrue(serializedShoppingList.isEmpty());

        // Do the same for their descriptions, which include the quantities...
        List<String> shoppingListDescs = Arrays.asList(refit.getShoppingListDescription());
        // ... except the second list needs to be mutable.
        List<String> serializedShoppingListDescs = new ArrayList<>(
                Arrays.asList(deserialized.getShoppingListDescription()));

        assertEquals(shoppingListDescs.size(), serializedShoppingListDescs.size());
        for (String desc : shoppingListDescs) {
            assertTrue(serializedShoppingListDescs.remove(desc));
        }

        assertTrue(serializedShoppingListDescs.isEmpty());
    }

    @Test
    public void fleaFLE4toFLE15Test() {
        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getFleaFLE4();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getFleaFLE15();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        assertEquals(mockCampaign, refit.getCampaign());

        //
        // Flea 4 to 15 Class D refit steps (in no particular order):
        // 1. Remove excess Large Laser (RA) [120 mins]
        // 2. Move Small Laser (LA) to (LT)(R) [120 mins]
        // 3. Move Small Laser (LA) to (RT)(R) [120 mins]
        // 4. Add Medium Laser (LA) [120 mins]
        // 5. Add Medium Laser (RA) [120 mins]
        // 6. Add Machine Gun (LA) [120 mins]
        // 7. Add Machine Gun (RA) [120 mins]
        // 8. Add Machine Gun Ammo [Full] Bin to (CT) [120 mins]
        // 9. Add 16 points of armor to 10 locations (except the HD).
        // a. Add 1 point to (LA) [5 mins]
        // b. Add 1 point to (RA) [5 mins]
        // c. Add 2 points to (LT) [10 mins]
        // d. Add 2 points to (RT) [10 mins]
        // e. Add 3 points to (CT) [15 mins]
        // g. Add 1 point to (LL) [5 mins]
        // h. Add 1 point to (RL) [5 mins]
        // i. Add 2 points to (RTL) [10 mins]
        // j. Add 2 points to (RTR) [10 mins]
        // k. Add 1 point to (RTC) [5 mins]
        // 10. Switch Flamer (CT) facing to (CT)(R) [120 mins]
        //
        // Everything else is the same.
        //

        // Per SO p188:
        // "This kit permits players to install a new item
        // where previously there was none..."
        assertEquals(Refit.CLASS_D, refit.getRefitClass());

        // Time?
        // + 1 removal @ 120 mins ea
        // + 2 moves @ 120 mins ea
        // + 1 facing change @ 120 mins ea
        // + 5 adds @ 120 mins ea
        // + 16 armor changes @ 5 mins ea
        // x 3 (Class D)
        assertEquals(((120.0 * 9.0) + (5.0 * 16.0)) * 3.0, refit.getActualTime(), 0.1);

        // Cost?
        // + 2 Medium Lasers @ 40,000 ea
        // + 2 Machine Guns @ 5,000 ea
        // + 1 ton Machine Gun Ammo @ 1,000 ea
        // + 1 ton Armor (Standard) @ 10,000 ea
        // x 1.1 (Refit Kit cost, SO p188)
        assertEquals(Money.of(40000.0 + 40000.0 + 5000.0 + 5000.0 + 1000.0 + 10000.0).multipliedBy(1.1),
                refit.getCost());

        // We're removing 1 Large Laser and using existing armor in 10 locations
        List<Part> removedParts = refit.getOldUnitParts();
        assertEquals(11, removedParts.size());
        assertEquals(1, removedParts.stream()
                .filter(p -> (p instanceof EquipmentPart) && p.getName().equals("Large Laser"))
                .count());
        assertEquals(10, removedParts.stream().filter(p -> (p instanceof Armor)).count());

        // All of the new parts should be from the old unit
        List<Part> newParts = refit.getNewUnitParts();
        assertTrue(newParts.stream().allMatch(p -> p.getUnit().equals(oldUnit)));

        // We need to buy two Medium Lasers, two Machine Guns, and Machine Gun Ammo
        List<Part> shoppingCart = refit.getShoppingList();
        assertEquals(2, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Medium Laser"))
                .count());
        assertEquals(2, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Machine Gun"))
                .count());
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("Machine Gun Ammo [Full] Bin"))
                .count());

        // We should have 16 points of standard armor on order
        assertNotNull(refit.getNewArmorSupplies());
        assertEquals(refit.getNewArmorSupplies().getType(), EquipmentType.T_ARMOR_STANDARD);
        assertEquals(16, refit.getNewArmorSupplies().getAmountNeeded());
    }

    @Test
    public void testFleaFLE4toFLE15WriteToXml() throws ParserConfigurationException, SAXException, IOException {
        Person mockTech = mock(Person.class);
        UUID techId = UUID.randomUUID();
        when(mockTech.getId()).thenReturn(techId);

        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getFleaFLE4();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getFleaFLE15();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Make sure the unit parts have an ID before we serialize them
        int partId = 1;
        for (Part part : oldUnit.getParts()) {
            part.setId(partId++);
        }

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        refit.setTech(mockTech);
        refit.addTimeSpent(60); // 1 hour of work!

        // Write the Refit XML
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        refit.writeToXML(pw, 0);

        // Get the Refit XML
        String xml = sw.toString();
        assertFalse(xml.isBlank());

        // Using factory get an instance of document builder
        DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element refitElt = xmlDoc.getDocumentElement();
        assertEquals("refit", refitElt.getNodeName());

        // Deserialize the refit
        Refit deserialized = Refit.generateInstanceFromXML(refitElt, new Version(), mockCampaign, oldUnit);
        assertNotNull(deserialized);
        deserialized.reCalc();

        // Spot check the values
        assertEquals(refit.getTime(), deserialized.getTime());
        assertEquals(refit.getActualTime(), deserialized.getActualTime());
        assertEquals(refit.getCost(), deserialized.getCost());
        assertEquals(refit.isSameArmorType(), deserialized.isSameArmorType());
        assertEquals(refit.hasFailedCheck(), deserialized.hasFailedCheck());
        assertEquals(refit.getRefitClass(), deserialized.getRefitClass());
        assertEquals(refit.getTimeSpent(), deserialized.getTimeSpent());
        assertEquals(refit.getTimeLeft(), deserialized.getTimeLeft());
        assertEquals(refit.isCustomJob(), deserialized.isCustomJob());
        assertEquals(refit.kitFound(), deserialized.kitFound());
        assertEquals(refit.isBeingRefurbished(), deserialized.isBeingRefurbished());
        assertEquals(refit.getTech().getId(), deserialized.getTech().getId());

        // Check that we got all the correct old parts in the XML
        Set<Integer> oldUnitParts = refit.getOldUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        Set<Integer> serializedOldParts = deserialized.getOldUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        assertEquals(oldUnitParts, serializedOldParts);

        // Check that we got all the correct new parts in the XML
        Set<Integer> newUnitParts = refit.getNewUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        Set<Integer> serializedNewParts = deserialized.getNewUnitParts().stream()
                .map(Part::getId)
                .collect(Collectors.toSet());
        assertEquals(newUnitParts, serializedNewParts);

        // Check that we got all the shopping list entries (by name, not amazing but reasonable)
        List<String> shoppingList = refit.getShoppingList().stream()
                .map(Part::getName)
                .collect(Collectors.toList());
        List<String> serializedShoppingList = deserialized.getShoppingList().stream()
                .map(Part::getName)
                .collect(Collectors.toList());

        // Make sure they're the same length first...
        assertEquals(shoppingList.size(), serializedShoppingList.size());

        // ... then make sure they're the "same" by removing them one by one...
        for (String partName : shoppingList) {
            assertTrue(serializedShoppingList.remove(partName));
        }

        // ... and ensuring nothing is left.
        assertTrue(serializedShoppingList.isEmpty());

        // Do the same for their descriptions, which include the quantities...
        List<String> shoppingListDescs = Arrays.asList(refit.getShoppingListDescription());
        // ... except the second list needs to be mutable.
        List<String> serializedShoppingListDescs = new ArrayList<>(
                Arrays.asList(deserialized.getShoppingListDescription()));

        assertEquals(shoppingListDescs.size(), serializedShoppingListDescs.size());
        for (String desc : shoppingListDescs) {
            assertTrue(serializedShoppingListDescs.remove(desc));
        }

        assertTrue(serializedShoppingListDescs.isEmpty());

        // Make sure the new armor is serialized/deserialized properly
        assertNotNull(deserialized.getNewArmorSupplies());
        assertTrue(refit.getNewArmorSupplies().isSameType(deserialized.getNewArmorSupplies()));
        assertEquals(refit.getNewArmorSupplies().getAmountNeeded(),
                deserialized.getNewArmorSupplies().getAmountNeeded());
    }

    @Test
    public void heavyTrackedApcMgToStandard() throws EntityLoadingException, IOException {
        final Hangar mockHangar = mock(Hangar.class);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        final ShoppingList mockShoppingList = mock(ShoppingList.class);
        when(mockCampaign.getShoppingList()).thenReturn(mockShoppingList);

        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getHeavyTrackedApcMg();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getHeavyTrackedApcStandard();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        assertEquals(mockCampaign, refit.getCampaign());

        // We're removing 4 Machine Guns and a Full Bin of Machine Gun Ammo
        List<Part> removedParts = refit.getOldUnitParts();
        assertEquals(5, removedParts.size());
        assertEquals(4, removedParts.stream()
                .filter(p -> (p instanceof EquipmentPart) && p.getName().equals("Machine Gun"))
                .count());
        assertEquals(1, removedParts.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("Machine Gun Ammo [Full] Bin"))
                .count());

        // All of the new parts (except ammo bins) should be from the old unit
        List<Part> newParts = refit.getNewUnitParts();
        assertTrue(newParts.stream().filter(p -> !(p instanceof AmmoBin)).allMatch(p -> p.getUnit().equals(oldUnit)));

        // We have nothing we need to buy
        List<Part> shoppingCart = refit.getShoppingList();
        assertTrue(shoppingCart.isEmpty());

        // We should not have any ammo needed
        assertNull(refit.getNewArmorSupplies());

        // Begin the refit
        refit.begin();

        // Complete the refit!
        String report = refit.succeed();
        assertNotNull(report);
    }

    @Test
    public void testMasakariAtoMasakariB() {
        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getMasakariWarhawkA();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("Test Player");
        oldEntity.setOwner(mockPlayer);

        // Create the entity we're going to refit to
        Entity newEntity = UnitTestUtilities.getMasakariWarhawkB();

        // Create the unit which will be refit
        Unit oldUnit = new Unit(oldEntity, mockCampaign);
        oldUnit.setId(UUID.randomUUID());
        oldUnit.initializeParts(false);

        // Create the Refit
        Refit refit = new Refit(oldUnit, newEntity, false, false);
        assertEquals(mockCampaign, refit.getCampaign());

        // Omni reconfiguration
        assertEquals(Refit.CLASS_OMNI, refit.getRefitClass());

        // Time?
        // Omni reconfig = 120 minutes here
        assertEquals(120.0, refit.getActualTime(), 0.1);

        // Cost?
        assertEquals(Money.of(316000).multipliedBy(1.1),
                refit.getCost());

        // We're removing 1 Large Laser and using existing armor in 10 locations
        List<Part> removedParts = refit.getOldUnitParts();
        assertEquals(9, removedParts.size());
        assertEquals(2, removedParts.stream()
                .filter(p -> (p instanceof EquipmentPart) && p.getName().equals("ER Large Laser"))
                .count());
        assertEquals(0, removedParts.stream().filter(p -> (p instanceof Armor)).count());

        // All of the new parts should be from the old unit
        List<Part> newParts = refit.getNewUnitParts();
        assertTrue(newParts.stream().allMatch(p -> p.getUnit().equals(oldUnit)));

        // We need to buy:
        // - 1 clan gauss rifle with 2 ammo bins,
        // - 3 ER medium lasers,
        // - 1 ER small laser,
        // - 2 SRM-6 launchers sharing 5 Narc-capable ammo bins,
        // - 1 NARC launcher with 1 ton of pods
        List<Part> shoppingCart = refit.getShoppingList();
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Gauss Rifle"))
                .count());
        assertEquals(2, shoppingCart.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("Gauss Rifle Ammo [Clan] Bin"))
                .count());
        assertEquals(3, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("ER Medium Laser"))
                .count());
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("ER Small Laser"))
                .count());
        assertEquals(2, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("SRM 6"))
                .count());
        assertEquals(5, shoppingCart.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("SRM 6 (Clan) Narc-capable Ammo Bin"))
                .count());
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Narc"))
                .count());
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("Narc Pods Bin"))
                .count());

        // We should have 0 points of standard armor on order
        assertNull(refit.getNewArmorSupplies());
    }
}

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import megamek.common.Entity;
import megamek.common.IPlayer;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.Warehouse;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;

public class RefitTest {
    @Test
    public void deserializationCtor() {
        Refit refit = new Refit();
        assertNotNull(refit);
    }

    @Test
    public void newRefitCtor() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);

        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getLocustLCT1V();
        IPlayer mockPlayer = mock(IPlayer.class);
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

        // ...and new parts.
        assertFalse(refit.getNewUnitParts().isEmpty());

        // ...and we'll need to buy some parts
        assertFalse(refit.getShoppingList().isEmpty());
    }

    @Test
    public void locust1Vto1ETest() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);

        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getLocustLCT1V();
        IPlayer mockPlayer = mock(IPlayer.class);
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
        //     1. Remove excess Machine Gun (LA) [120 mins]
        //     2. Remove excess Machine Gun (RA) [120 mins]
        //     3. Remove Machine Gun Ammo Bin (CT) [120 mins]
        //     4. Move Medium Laser (CT) to (RA) [120 mins]
        //     5. Add Medium Laser to (LA) [120 mins]
        //     6. Add Small Laser to (RA) [120 mins]
        //     7. Add Small Laser to (LA) [120 mins]
        //
        // Everything else is the same.
        //

        // TODO: confirm this
        assertEquals(Refit.CLASS_D, refit.getRefitClass());

        // Time?
        //     + 3 removals @ 120 mins ea
        //     + 1 move @ 120 mins ea
        //     + 3 adds @ 120 mins ea
        //     x 3 (Class D)
        assertEquals((120.0 * 7.0) * 3.0, refit.getActualTime(), 0.1);

        // Cost?
        //    + 1 Medium Laser @ 40,000 ea
        //    + 2 Small Lasers @ 11,250 ea
        //    x 1.1 (Not Custom)
        assertEquals(Money.of((40000 + 11250 + 11250) * 1.1), refit.getCost());

        // We're removing 2 machine guns and an ammo bin
        List<Part> removedParts = refit.getOldUnitParts();
        assertEquals(3, removedParts.size());
        assertEquals(2, removedParts.stream()
                .filter(p -> (p instanceof EquipmentPart) && p.getName().equals("Machine Gun")).count());
        assertEquals(1, removedParts.stream()
                .filter(p -> (p instanceof AmmoBin) && p.getName().equals("Machine Gun Ammo Bin")).count());

        // All of the new parts should be from the old unit
        List<Part> newParts = refit.getNewUnitParts();
        assertTrue(newParts.stream().allMatch(p -> p.getUnit().equals(oldUnit)));

        // We need to buy one Medium Laser and two Small Lasers
        List<Part> shoppingCart = refit.getShoppingList();
        assertEquals(1, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Medium Laser")).count());
        assertEquals(2, shoppingCart.stream()
                .filter(p -> (p instanceof MissingEquipmentPart) && p.getName().equals("Small Laser")).count());
    }

    @Test
    public void testBasicRefitWriteToXml() throws ParserConfigurationException, SAXException, IOException {
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getEntities()).thenReturn(new ArrayList<>());
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);

        // Create the original entity backing the unit
        Entity oldEntity = UnitTestUtilities.getLocustLCT1V();
        IPlayer mockPlayer = mock(IPlayer.class);
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
        refit.writeToXml(pw, 0);

        // Get the Refit XML
        String xml = sw.toString();
        assertFalse(xml.trim().isEmpty());

        // Using factory get an instance of document builder
        DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

        // Parse using builder to get DOM representation of the XML file
        Document xmlDoc = db.parse(new ByteArrayInputStream(xml.getBytes()));

        Element refitElt = xmlDoc.getDocumentElement();
        assertEquals("refit", refitElt.getNodeName());

        // Deserialize the refit
        Refit deserialized = Refit.generateInstanceFromXML(refitElt, oldUnit, new Version("1.0.0"));
        assertNotNull(deserialized);

        // Spot check the values
        assertEquals(refit.getActualTime(), deserialized.getActualTime());
        assertEquals(refit.getCost(), deserialized.getCost());
    }
}

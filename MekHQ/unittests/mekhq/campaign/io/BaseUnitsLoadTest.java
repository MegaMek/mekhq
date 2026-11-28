/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.io;

import static mekhq.campaign.enums.CampaignTransportType.SHIP_TRANSPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.force.Formation;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A load treats the units kept at a base like the main force's: they join the game and the transporter maps, and they
 * keep their place in their formations.
 */
class BaseUnitsLoadTest {
    private static final String SYSTEM_ID = "Galatea";

    private PlanetarySystem system;
    private PartsScenario scenario;
    private Campaign campaign;
    private PlayerBase base;

    @BeforeEach
    void setUp() {
        system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn(SYSTEM_ID);
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        base = new PlayerBase(new FixedLocation(system));
        campaign.getCampaignLocationManager().addPlayerBase(base);
    }

    @AfterEach
    void tearDown() {
        Systems.setInstance(null);
    }

    private Unit keepAtTheBase(UnitFixture fixture) {
        Unit unit = scenario.withUnit(fixture);
        campaign.getPlayerForce().getHangar().removeUnit(unit.getId());
        base.getBaseHangar().addUnit(unit);
        return unit;
    }

    @Test
    void cleanUpKeepsAUnitAtABaseInItsFormation() {
        Unit unit = keepAtTheBase(UnitFixture.LOCUST_LCT_1V);
        Formation lance = new Formation("Lance");
        lance.setId(5);
        campaign.getPlayerForce().importFormation(lance);
        lance.addUnit(unit.getId());
        unit.setFormationId(lance.getId());

        campaign.cleanUp();

        assertTrue(lance.getUnits().contains(unit.getId()), "the unit at the base is still in its formation");
    }

    @Test
    void aLoadedBaseRegistersTheUnitsItKeeps() throws Exception {
        Unit dropShip = keepAtTheBase(UnitFixture.LEOPARD_DROPSHIP);
        String basesXml = basesXml();

        Systems systems = mock(Systems.class);
        when(systems.getSystemById(SYSTEM_ID)).thenReturn(system);
        Systems.setInstance(systems);
        Campaign reloaded = PartsScenario.create().getCampaign();
        assertFalse(reloaded.hasTransports(SHIP_TRANSPORT), "the fresh campaign has no transports of its own");

        processPlayerBaseNodes(reloaded, basesXml);

        Unit reloadedDropShip = reloaded.getUnit(dropShip.getId());
        assertNotNull(reloadedDropShip, "the DropShip came back at its base");
        PlayerBase reloadedBase = reloaded.getCampaignLocationManager().getPlayerBases().iterator().next();
        assertSame(reloadedDropShip, reloadedBase.getBaseHangar().getUnit(dropShip.getId()));
        Entity entity = reloadedDropShip.getEntity();
        assertNotEquals(Entity.NONE, entity.getId(), "the DropShip has an entity id");
        assertSame(entity, reloaded.getGame().getEntity(entity.getId()), "the DropShip joined the game");
        assertTrue(reloaded.hasTransports(SHIP_TRANSPORT), "the DropShip is a transport the campaign knows");
        assertEquals(List.of(reloadedDropShip),
              List.copyOf(reloaded.getTransportsByType(SHIP_TRANSPORT,
                    reloaded.getTransports(SHIP_TRANSPORT).keySet().iterator().next())));
    }

    private String basesXml() {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("<playerBases>");
        base.writeToXML(printWriter, 1);
        printWriter.println("</playerBases>");
        printWriter.flush();
        return writer.toString();
    }

    private static void processPlayerBaseNodes(Campaign campaign, String basesXml) throws Exception {
        Node root = DocumentBuilderFactory.newInstance()
                          .newDocumentBuilder()
                          .parse(new ByteArrayInputStream(basesXml.getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        Method method = CampaignXmlParser.class.getDeclaredMethod("processPlayerBaseNodes", Campaign.class,
              Node.class, Version.class);
        method.setAccessible(true);
        method.invoke(null, campaign, root, new Version());
    }
}
